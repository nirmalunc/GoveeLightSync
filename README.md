# GoveeLightSync
No need to buy a DreamView device or camera to support light sync for Govee products! Now you can natively sync any Govee lights to your display.

Demo Video: https://tinyurl.com/LightSync

\
**3 Different Options to Sync Lights:**

**Option 1 (EASIEST and no coding required): LAN Control GUI**

*Required Prerequisites:*
[JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23)
 1. Turn on your Govee Lights
 2. Open the Govee App and turn on "LAN Control" under device settings (the app can be buggy, this [thread](https://www.reddit.com/r/Govee/comments/wsupdh/lan_control_option_doesnt_appear_in_the_app/) has common solutions)
 3. Install 'SyncApp.exe' from the repository (Look above ^)
 4. Run it!
 5. If you get a Java error, you likely need to save JAVA_HOME in your system variables: [Instructions](https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html)

\
**Option 2: LAN Control (fast, but not available on all devices and sometimes unavailable on the Govee app)**

*Required Prerequisites:*
[JDK](https://docs.oracle.com/en/java/javase/17/install/overview-jdk-installation.html), [Apache Maven](https://maven.apache.org/download.cgi)
 1. Turn on your Govee Lights
 2. Open the Govee App and turn on "LAN Control" under device settings
 3. After cloning repo, run `mvn install` in your terminal and you should see "BUILD SUCCESS"
 4. Navigate to `\src\main\java\com\nirmal\LANControlScripts\GetLANControlDetails.java`
 5. Run `GetLANControlDetails.java` and copy the device IP from the output
 6. Replace `DEVICE_IP` in `LANControl.java` with your copied device IP
 7. Run the `LANControl.java` file and your lights should be synced to your display!

\
**Option 3: Govee API (slower, but supported by more devices and always available)**

*Required Prerequisites:*
[JDK](https://docs.oracle.com/en/java/javase/17/install/overview-jdk-installation.html), [Apache Maven](https://maven.apache.org/download.cgi), and [your Govee API Key](https://developer.govee.com/reference/apply-you-govee-api-key)
 1. Turn on your Govee Lights
 2. After cloning repo, rename your `.env.example` file to `.env` and replace `API_KEY`, `SKU`, and `DEVICE_ID` with your credentials
	 - Submit the following GET Request if you are unsure of your SKU and Device ID: 
		 `GET /router/api/v1/user/devices HTTP/1.1 Host: https://openapi.api.govee.com Content-Type: application/json Govee-API-Key: {API KEY}`
 3. Run `mvn install` in your terminal and you should see "BUILD SUCCESS"
 4. Run the `Main.java` file and your lights should be synced to your display!

\
*For those who want to play around with the performance and color change threshold, look for the "MODIFIABLE" comments in `Main.java` and `LANControl.java`*

\
***10/2 Patch Update - User-friendly GUI for LAN Control!***
- Added a simple GUI application to control lights from any PC
- Install, run, and sync your lights!

***9/28 Patch Update - Better color perception for dark scenes!***
- Changed color calculation algorithm to weigh colors based on luminance for better perceptual accuracy 

***9/27 Patch Update - EVEN MORE Reduced Latency using LAN Control!***
- Everything needed to sync through LAN Control is inside `src\main\java\com\nirmal\LANControlScripts` directory
- Reduces average latency to ≈ 50ms depending on hardware capability

***9/25 Patch Update - Reduced Latency!***
- Upgraded to HTTPClient to leverage HTTP/2
- Reduced # of pixels processed per calculation
- Asynchronous POST Requests with timeout to prevent bottlenecks
- Lowered default color change threshold since it is now capable of handling color changes more often

***Planned Future Updates:***
- Reduced latency delay ✅
- More user-friendly support ✅
- Ability to use screen edges for color sync ⭐
- More gradual color syncing for drastic scene changes ⭐
- Match segments of lights to the corresponding areas of the screen ⭐
- User Suggestions: https://forms.gle/NK6y5NysLNNfNUKC7
