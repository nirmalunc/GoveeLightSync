package com.nirmal;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    private static final int THRESHOLD = 15; // MODIFIABLE: THRESHOLD SET TO 15 BY DEFAULT!

    private static final Dotenv dotenv = Dotenv.load();
    
    // Define Govee API endpoint and headers
    private static final String URL = "https://openapi.api.govee.com/router/api/v1/device/control";
    private static final String API_KEY = dotenv.get("API_KEY");
    private static final String SKU = dotenv.get("SKU");
    private static final String DEVICE_ID = dotenv.get("DEVICE_ID");

    private static volatile int[] prevColor = {0, 0, 0};
    private static volatile int[] newColor = {0, 0, 0};
    private static volatile boolean colorChanged = false;

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // 1 thread for checking color and 1 for sending requests
    private static final HttpClient client = HttpClient.newBuilder()
                                                       .version(HttpClient.Version.HTTP_2)
                                                       .build();

    private static JSONObject data = new JSONObject();
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

    // Updates color parameter and calls POST request function
    public static void setColor(int rgbNumber) throws IOException {
        capability.put("value", rgbNumber);

        // Send the updated JSON object
        sendPostRequest(data.toString());
    }

    // Sends post request
    private static void sendPostRequest(String jsonData) throws IOException {
        CompletableFuture.runAsync(() -> {
            try {
                // long startTime = System.nanoTime(); // API Request Timer: Uncomment to time length of API requests
                
                HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .header("Content-Type", "application/json")
                            .header("Govee-API-Key", API_KEY)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonData, StandardCharsets.UTF_8))
                            .build();
    
                // Send the request asynchronously
                client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .orTimeout(250, TimeUnit.MILLISECONDS) // Timeout if the request takes too long
                // .thenAccept(response -> {
                //     long endTime = System.nanoTime();
                //     System.out.println("Program took " + (endTime - startTime) / 1000000 + " milliseconds");
                // }) // API Request Timer: Uncomment to time length of API requests
                .exceptionally(e -> {
                //  e.printStackTrace(); // Optional: Print stack trace when connection times out
                    return null;
                });
    
            } catch (Exception e) {
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

        // Thread 1: Color calculation, running every 150ms by default
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
        }, 0, 180, TimeUnit.MILLISECONDS); // MODIFIABLE: INCREASE OR DECREASE TIME BASED ON COMPUTER PERFORMANCE (Recommend keeping thread 2's time lower than thread 1)

        // Thread 2: Threshold checker, running every 30ms by default
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (newColor) {
                // Check if the threshold is surpassed and color changed
                if (colorChanged && isThresholdSurpassed()) {
                    int rgbNumber = ((newColor[0] & 0xFF) << 16) | ((newColor[1] & 0xFF) << 8) | (newColor[2] & 0xFF);

                    // Submit the task to update the color
                    try {
                        setColor(rgbNumber);
                        // System.out.println("Color Changed to: " + rgbNumber); // Optional: Uncomment if you want to be notified each time the color changes
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
        }, 0, 30, TimeUnit.MILLISECONDS); // MODIFIABLE: INCREASE OR DECREASE TIME BASED ON COMPUTER PERFORMANCE (Recommend keeping thread 2's time lower than thread 1)
    }
}