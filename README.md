# IPTV Android app

A tiny full-screen wrapper around your IPTV server (works on phones, tablets and Android TV).
On first start it asks for the server address, e.g. `http://192.168.1.50:8080`, and remembers it.
Press the Menu key (or relaunch after a connection error) to change the address. Back twice quickly = exit.

## Get the APK without installing anything (GitHub)
1. Create a free repository on github.com and upload everything from this folder (keep the `.github` folder).
2. Open the **Actions** tab -> "Build APK" -> wait ~3 minutes for the green check.
3. Open the finished run, download **IPTV-apk**, unzip it, copy `app-debug.apk` to your phone and open it
   (allow "install unknown apps" when Android asks).

## Or with Android Studio
Open this folder, then Build -> Build APK(s). The file is `app/build/outputs/apk/debug/app-debug.apk`.
