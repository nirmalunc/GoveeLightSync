# GoveeLightSync
No need to buy a DreamView device or camera to support light sync for Govee products! Now you can natively sync any Govee lights to your display.

**9/25 Patch Update - Reduced Latency!**
- Upgraded to HTTPClient to leverage HTTP/2
- Reduced # of pixels processed per calculation
- Asynchronous POST Requests with timeout to prevent bottlenecks
- Lowered default color change threshold since it is now capable of handling color changes more often

**Required Prerequisites:**
[JDK](https://docs.oracle.com/en/java/javase/17/install/overview-jdk-installation.html), [Apache Maven](https://maven.apache.org/download.cgi), and [your Govee API Key](https://developer.govee.com/reference/apply-you-govee-api-key)

**Steps to Sync Lights:**

 1. Turn on your Govee Lights
 2. Rename your `.env.example` file to `.env` and replace `API_KEY`, `SKU`, and `DEVICE_ID` with your credentials
	 - Submit the following GET Request if you are unsure of your SKU and Device ID: 
		 `GET /router/api/v1/user/devices HTTP/1.1 Host: https://openapi.api.govee.com Content-Type: application/json Govee-API-Key: {API KEY}`
 3. Run `mvn install` in your terminal and you should see "BUILD SUCCESS"
 4. Run the `Main.java` file and your lights should be synced to your display!

*For those who want to play around with the performance and color change threshold, look for the "MODIFIABLE" comments in `Main.java`*

***Planned Future Updates:***
- Reduced latency delay ✅
- More user-friendly support ⭐
- Ability to use screen edges for color sync ⭐
- User Suggestions: https://forms.gle/NK6y5NysLNNfNUKC7
