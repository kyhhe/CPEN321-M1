# CPEN321 M1 - App skeleton
_Setup, build, and run instructions for CPEN 321 M1._

This repository supports two ways to run and evaluate the application:
1. **[Method 1: Pre-built APK + Hosted Cloud Backend](#method-1-pre-built-apk--hosted-cloud-backend)**: No local backend or Android build setup required.
2. **[Method 2: Run Full Stack Locally on Your Own Machine](#method-2-run-full-stack-locally-on-your-machine)**: Run the backend and build/run the Android app entirely on localhost.

---

## Method 1: Pre-built APK + Hosted Cloud Backend

Use this method to evaluate the application immediately without installing Node.js, Docker, or building Android source code.

### 1. Prerequisites
- An active Android Emulator OR a physical Android device.
   - Note: This app has only been tested on the **Pixel 9** as the device and **Android Baklava (API level 36)** as the system image, as indicated in the submission guidelines
- [Android Platform Tools (`adb`)](https://developer.android.com/tools/releases/platform-tools) installed (or Android Studio running an emulator).

### 2. Verify Cloud Backend Status
The backend is continuously hosted on a Google Cloud Compute Engine VM (`e2-micro`, Ubuntu 24.04).
- **HTTP Health Check:** Open [http://136.67.224.94:3000/health](http://136.67.224.94:3000/health) in your browser.  
  Expected response: `{"status":"ok"}`
- **WebSocket Stream Relay:** Active on `ws://136.67.224.94:3000/pixels`.

### 3. Install and Run the APK
The pre-built release APK is compiled configured to communicate with the hosted cloud backend.

- **Location:** `M1_Release.apk` in the repository root
- **Installation**
  1. Start your Android Emulator.
  2. Drag `M1_Release.apk` from your file explorer and drop it onto the emulator screen. The app **CPEN321 Application** will install automatically, or 
  ```bash
  adb install M1_Release.apk
  ```
- **Launch the app**

---

## Method 2: Run Full Stack Locally on Your Machine

Use this method if you want to inspect, modify, and run both the backend and frontend locally.

### 1. Prerequisites
- [Git](https://git-scm.com/install/)
- [Node.js](https://nodejs.org/en/download/) (v22+) & [npm](https://docs.npmjs.com/) (v10+)
- [Docker](https://docs.docker.com/desktop/) (optional, if running backend in a container)
- [Java 17](https://adoptium.net/temurin/releases/?version=17) (JDK 17)
- [Android Studio](https://developer.android.com/studio) with Android SDK Platform 35

---

### 2. Start the Local Backend

#### Configure Environment
Copy the example environment file:
```bash
cp backend/.env.example backend/.env
```
Ensure `backend/.env` has:
```dotenv
PORT=3000
NODE_ENV=development
JWT_SECRET=your_jwt_secret_key_here
```
The database unused and can be left blank.


#### Option A: Run via Docker (Using Dockerfile)
Build and run the standalone backend container directly:
```bash
cd backend

# Build Docker image:
docker build -t cpen321-backend .

# Run container on port 3000:
docker run -d --name cpen321-backend -p 3000:3000 --env-file .env cpen321-backend
```

To stop the container:
```bash
docker stop cpen321-backend && docker rm cpen321-backend
```

#### Option B: Run Directly with Node.js
```bash
cd backend
npm install
npm run dev
```

Verify the local backend is up: [http://localhost:3000/health](http://localhost:3000/health)

---

### 3. Configure & Run the Frontend

#### Configure `local.properties`
Create `frontend/local.properties` (gitignored):
```bash
cp frontend/local.properties.example frontend/local.properties
```

Edit `frontend/local.properties`:
```properties
# Path to your Android SDK:
sdk.dir=/path/to/Android/sdk

# Points to your local machine from the Android Emulator:
API_BASE_URL=http://10.0.2.2:3000

# (If testing on a physical device on the same Wi-Fi, use your machine's LAN IP, e.g. http://192.168.1.50:3000)

# Google Android OAuth Client ID for Credential Manager:
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
```

#### Build & Run
- **Via Android Studio:**
  1. Open the `frontend/` folder in Android Studio.
  2. Sync Gradle dependencies (`File -> Sync Project with Gradle Files`).
  3. Select your emulator/device and click **Run** (Green play button).
- **Via Command Line:**
  ```bash
  # Run automated launch script:
  ./scripts/run-frontend.sh

  # Or build APKs manually:
  cd frontend
  ./gradlew assembleDebug
  ```

---

## Features

Once the app is launched (either via the pre-built APK or local build), test the three main components:

### Button 1: Login & Connect
*Ensure there is a Google Account on the device. Sign in to a valid Google account through the Play Store prior to testing this button* 
- Tap **"Login & Connect"**.
- Tap **"Sign in with Google"** and complete authentication.
- Verifies display of:
  - **Server IP Address** & **Server Local Time** (retrieved from backend `/server-ip` and `/server-time`).
  - **Client IP Address** & **Client Local Time** (queried on device).
  - **Developer Name** (retrieved from backed `/my-name`).
  - **Authenticated User Name** (retrieved from Google ID token).

### Button 2: Live Pixel Art
- Tap **"Live Pixel Art"**.
- The backend connects to the upstream course WebSocket (`ws://8.229.22.124`) and streams pixel updates (`{x, y, color}`) to the client.
- Verifies real-time incremental assembly of a 16x16 pixel art image on a Jetpack Compose Canvas.

### Button 3: Timer & Surprise (Pokémon Gacha)
- Tap **"Timer & Surprise"**.
- Enter a duration (e.g. 0 min, 5 sec) and tap **"Start Timer"**.
- Once the countdown reaches 00:00, Pokéball appears.
- Tap the Pokéball to trigger a sequence of shake animations, followed by an opening burst.
- A Pokémon is pulled using a 4-tier gacha system (`3★ Common`, `4★ Rare`, `5★ Epic`, `6★ Exclusive`) queried from PokéAPI.
- Tap the card to inspect details (Type, Height, Weight, BST) and tap **"View Collection"** to view all saved Pokémon persisted via SharedPreferences.

---
