ScanBuddy is an Android app that helps children get ready for medical imaging scans: MRI, CT, X-ray, and ultrasound. It uses clear explanations, comforting visuals, realistic scan sounds, and an interactive “Stay Still Challenge” to show what to expect and help kids practice staying calm and still. All content (text, images, and sounds) is bundled with the app, so it works fully offline.

Features:

-Brief, kid-friendly guides for MRI, CT, X-ray, and ultrasound

-Optional machine sound previews stored in res/raw

-“Stay Still Challenge” that uses the device accelerometer

-UI built with Jetpack Compose

How it works (offline)
Educational text and images are in res/drawable and the code.
Audio files (e.g., mri_demo.mp3, xray_demo.mp3) live in res/raw.
Sounds are played with MediaPlayer.create(context, R.raw.filename).
The Stay Still Challenge uses the accelerometer via SensorManager.

Installation:
Option A: Install the APK

Go to the repository’s Releases page.
Download app-release.apk.
On your Android device, allow installing from unknown sources when prompted.
Open the APK to install.

Option B: Build from source

Clone the repo:
git clone https://github.com/ChesapeakeCoder/ScanBuddyApp.git
Open in Android Studio
Select a device (emulator or phone) from Device Manager.
Run the app.
To generate a signed release APK:
Android Studio -> Build -> Generate Signed App Bundle / APK -> APK (Release)
