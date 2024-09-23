package com.nirmal;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    private static final Dotenv dotenv = Dotenv.load();
    // Define Govee API endpoint and headers
    private static final String URL = "https://openapi.api.govee.com/router/api/v1/device/control";
    private static final String API_KEY = dotenv.get("API_KEY");
    private static final String SKU = dotenv.get("SKU");
    private static final String DEVICE_ID = dotenv.get("DEVICE_ID");
    private static final int THRESHOLD = 25;
    private static volatile int[] prevColor = {0, 0, 0};
    private static volatile int[] newColor = {0, 0, 0};
    private static volatile boolean colorChanged = false;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static JSONObject data = new JSONObject(); // Initialize JSON
    private static JSONObject capability = new JSONObject();

    static {
        data.put("requestId", 1);
        JSONObject payload = new JSONObject();
        payload.put("sku", SKU);
        payload.put("device", DEVICE_ID);
        capability.put("type", "devices.capabilities.color_setting");
        capability.put("instance", "colorRgb");
        payload.put("capability", capability);
        data.put("payload", payload);
    }

    // Optional helper function to turn on lights
    public static void turnOn() throws IOException {
        JSONObject data = new JSONObject();
        data.put("requestId", UUID.randomUUID().toString());
        JSONObject payload = new JSONObject();
        payload.put("sku", SKU);
        payload.put("device", DEVICE_ID);
        JSONObject capability = new JSONObject();
        capability.put("type", "devices.capabilities.on_off");
        capability.put("instance", "powerSwitch");
        capability.put("value", 1);
        payload.put("capability", capability);
        data.put("payload", payload);

        sendPostRequest(data.toString());
    }

    // Optional helper function to turn off lights
    public static void turnOff() throws IOException {
        JSONObject data = new JSONObject();
        data.put("requestId", UUID.randomUUID().toString());
        JSONObject payload = new JSONObject();
        payload.put("sku", SKU);
        payload.put("device", DEVICE_ID);
        JSONObject capability = new JSONObject();
        capability.put("type", "devices.capabilities.on_off");
        capability.put("instance", "powerSwitch");
        capability.put("value", 0);
        payload.put("capability", capability);
        data.put("payload", payload);

        sendPostRequest(data.toString());
    }

    // Capture entire screen and calculate the average color
    public static int[] averageColor() throws AWTException {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenShot = robot.createScreenCapture(screenRect);

        long sumRed = 0, sumGreen = 0, sumBlue = 0;
        int width = screenShot.getWidth();
        int height = screenShot.getHeight();
        int numPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(screenShot.getRGB(x, y));
                sumRed += pixelColor.getRed();
                sumGreen += pixelColor.getGreen();
                sumBlue += pixelColor.getBlue();
            }
        }

        int avgRed = (int) (sumRed / numPixels);
        int avgGreen = (int) (sumGreen / numPixels);
        int avgBlue = (int) (sumBlue / numPixels);

        return new int[]{avgRed, avgGreen, avgBlue};
    }

    // Updates color parameter and calls POST request function
    public static void setColor(int rgbNumber) throws IOException {
        capability.put("value", rgbNumber);

        // Send the updated JSON object
        sendPostRequest(data.toString());
    }

    // Sends post request asynchronously
    private static void sendPostRequest(String jsonData) throws IOException {
        CompletableFuture.runAsync(() -> {
        try {
            URL url = new URL(URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Govee-API-Key", API_KEY);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try {
                connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            connection.disconnect();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    });
    }

    // Helper function to check if color should be updated based on threshold
    private static boolean isThresholdSurpassed() {
        return Math.abs(newColor[0] - prevColor[0]) > THRESHOLD ||
               Math.abs(newColor[1] - prevColor[1]) > THRESHOLD ||
               Math.abs(newColor[2] - prevColor[2]) > THRESHOLD;
    }

    public static void main(String[] args) throws AWTException, IOException {
        if (API_KEY == null || SKU == null || DEVICE_ID == null) {
            System.err.println("Error: Make sure you have set the API_KEY, SKU, and DEVICE_ID environment variables in the .env file!");
            System.exit(1);
        }
        System.out.println("API Key: " + API_KEY);
        System.out.println("SKU: " + SKU);
        System.out.println("Device ID: " + DEVICE_ID);
        // Thread 1: Color calculation, running every 100ms
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
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Thread 2: Threshold checker, running every 50ms
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (newColor) {
                // Check if the threshold is surpassed and color changed
                if (colorChanged && isThresholdSurpassed()) {
                    int rgbNumber = ((newColor[0] & 0xFF) << 16) | ((newColor[1] & 0xFF) << 8) | (newColor[2] & 0xFF);

                    // Submit the task to update the color
                    try {
                        setColor(rgbNumber);
                    } catch (IOException e) {
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
        }, 0, 50, TimeUnit.MILLISECONDS);
    }
}