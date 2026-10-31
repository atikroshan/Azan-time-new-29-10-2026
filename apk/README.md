# Offline Azan APK Releases

### Current Version: v3.0.72 (versionCode: 232)
- **Filename:** `app-v3.0.72.apk`
- **Build Type:** Release / Signed Debug Keystore
- **Changelog:**
  - **Fixed Silent/Vibrate Audio Skip Bug:** Removed `ringerMode` check in `AzanForegroundService.kt` that was skipping Azan playback when the phone was in Silent or Vibrate mode. Azan now plays out loud as an alarm even if the phone is on Silent or Vibrate!
  - **Alarm Clock Precision API (`setAlarmClock`):** Upgraded `AlarmScheduler.kt` to use Android's highest-priority `AlarmManager.setAlarmClock()` API to guarantee exact-second Azan triggering even during deep Doze mode or OEM battery optimization.
  - **Single Latest APK:** Cleaned up all older APK builds from the `/apk` directory.
