package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object GoogleSheetMasjidSync {
    const val DEFAULT_SHEET_URL = "https://docs.google.com/spreadsheets/d/13l1dJh64fyOnpHFlw81iWKHZlA5ko0JVWJ_qoF43k0g/export?format=csv"
    const val SHEET_EDIT_URL = "https://docs.google.com/spreadsheets/d/13l1dJh64fyOnpHFlw81iWKHZlA5ko0JVWJ_qoF43k0g/edit?usp=sharing"
    const val DRIVE_FOLDER_URL = "https://drive.google.com/drive/folders/1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n?usp=sharing"

    val DEFAULT_CSV_CONTENT = """
Masjid Name,Mohammadiya Masjid,,,,,
Address,Swagat Nagar,,,,,
ID,100111111,,,,,
Masjid Photo,https://drive.google.com/file/d/1QP6elu7nlZwmrxjG4p-eyZWOrEaEdiwo/view?usp=drive_link,,,,,
Masjid QR,https://drive.google.com/file/d/1V3IA8iNF5FJ-tT2zGMLYre5cebqbffpi/view?usp=drivesdk,,,,,
,Fajr,Zohar,Asr,Maghrib,Isha,Jummah
Azan,05:10,01:15,04:30,06:06,07:30,12:30
Jammat,05:40,01:30,04:45,06:09,07:45,01:30
Admin ID,admin,,,,,
Password,9960171516,,,,,
,,,,,,
Masjid Name,Hazrat Imam Hussain Masjid,,,,,
Address,Tai Chowk,,,,,
ID,100111112,,,,,
Masjid Photo,https://drive.google.com/file/d/1Wtt9FYQSWz2o0PpxuCtGJZjSKBOLNhjH/view?usp=drive_link,,,,,
Masjid QR,,,,,,
,Fajr,Zohar,Asr,Maghrib,Isha,Jummah
Azan,05:10,01:15,04:30,06:07,07:30,12:30
Jammat,05:40,01:30,05:45,06:10,07:45,01:30
Admin ID,admin,,,,,
Password,9970595659,,,,,
,,,,,,
Masjid Name,Abu Bakar Siddique Masjid,,,,,
Address,Mumtaz Nagar,,,,,
ID,100111113,,,,,
Masjid Photo,https://drive.google.com/file/d/1wtzGogJQgbTLR1rHlTAocBeMdwMESBtD/view?usp=drive_link,,,,,
Masjid QR,,,,,,
,Fajr,Zohar,Asr,Maghrib,Isha,Jummah
Azan,06:00,01:15,05:18,06:11,07:55,12:30
Jammat,06:22,01:30,05:32,06:13,08:05,01:30
Admin ID,admin,,,,,
Password,9371883412,,,,,
,,,,,,
Masjid Name,Hazrat Jang Bahadur Salabat Kha,,,,,
Address,"Bhayya Chowk, Railway Station Ground",,,,,
ID,100111114,,,,,
Masjid Photo,https://drive.google.com/file/d/1Ju4p3KkS6sc3vzSvf68Bm1panplC_g8r/view?usp=drive_link,,,,,
Masjid QR,,,,,,
,Fajr,Zohar,Asr,Maghrib,Isha,Jummah
Azan,05:42,01:15,05:18,06:11,07:55,12:30
Jammat,06:17,01:30,05:32,06:13,08:05,01:30
Admin ID,admin,,,,,
Password,9595996629,,,,,
""".trimIndent()

    fun extractGoogleDriveDirectUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return ""
        if (clean.contains("lh3.googleusercontent.com")) {
            return clean.replace("=w1000", "=w600-rw")
        }
        val idRegex = "(?:/file/d/|/d/|id=)([-_a-zA-Z0-9]{20,})".toRegex()
        val match = idRegex.find(clean)?.groupValues?.getOrNull(1)
        if (match != null) {
            return "https://lh3.googleusercontent.com/d/$match=w600-rw"
        }
        val fallbackRegex = "[-_a-zA-Z0-9]{25,}".toRegex()
        val fallbackMatch = fallbackRegex.findAll(clean).lastOrNull()?.value
        return if (fallbackMatch != null) {
            "https://lh3.googleusercontent.com/d/$fallbackMatch=w600-rw"
        } else {
            clean
        }
    }

    fun to24Hr(timeStr: String, isPm: Boolean): String {
        val clean = timeStr.trim()
        if (!clean.contains(":")) return clean
        val parts = clean.split(":")
        var h = parts[0].toIntOrNull() ?: return clean
        val m = parts[1].toIntOrNull() ?: return clean
        if (h in 0..23 && (h >= 13 || (h == 12 && isPm))) {
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        if (isPm) {
            if (h in 1..11) {
                h += 12
            }
        } else {
            if (h == 12) {
                h = 0
            }
        }
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatForCsv(time24: String?): String {
        if (time24.isNullOrBlank() || !time24.contains(":")) return "00:00"
        val parts = time24.trim().split(":")
        val rawH = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val h12 = when {
            rawH == 0 -> 12
            rawH > 12 -> rawH - 12
            else -> rawH
        }
        return String.format(Locale.US, "%02d:%02d", h12, m)
    }

    var APPS_SCRIPT_WEBAPP_URL = "https://script.google.com/macros/s/AKfycbwFuuMm1an264fUDp9DvaOnjbvZR-FYUgv_uGYqDY338dACKU2Xdnyv2PHrDEr-KeqM/exec"

    val APPS_SCRIPT_SAMPLE_CODE = """
// ==============================================================================================
// AZAN TIME SOLAPUR - GOOGLE APPS SCRIPT (FULL BACKEND SCRIPT)
// Root Folder Name: "Masjid Details" (ID: 1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n)
//
// Features & Fixes:
// 1. Root folder is "Masjid Details"
// 2. On new masjid request:
//    - Creates subfolder named: <masjid_slug>_<id> (e.g. mohomadiya_100111115)
//    - Inside subfolder saves:
//        * Photo as <masjid_slug>_photo.jpg (e.g. mohomadiya_photo.jpg)
//        * QR as <masjid_slug>_qr.jpg (e.g. mohomadiya_qr.jpg)
//    - Generates shareable view links and pastes them into Google Sheet
// 3. Strictly enforces 10 rows format for every Masjid:
//    Row 1: Masjid Name
//    Row 2: Address
//    Row 3: ID (auto sequential 9-digit e.g. 100111115)
//    Row 4: Masjid Photo (Drive URL)
//    Row 5: Masjid QR (Drive URL)
//    Row 6: [Blank], Fajr, Zohar, Asr, Maghrib, Isha, Jummah
//    Row 7: Azan times
//    Row 8: Jammat times
//    Row 9: Admin ID
//    Row 10: Password
//    Row 11: [Blank separator row]
// 4. One-click migration function `organizeExistingMasjidDetails()` to move & rename the 4
//    existing photos into their individual folders (<slug>_<id>/<slug>_photo.jpg)
//    and auto-repair any 9-row or missing ID blocks in Sheet1.
// 5. Update timings (updateAll, update), ratings, Taqwa backup & restore.
// ==============================================================================================

// Helper: Get or find the "Masjid Details" root folder
function getMasjidDetailsFolder() {
  // 1. Try finding by name "Masjid Details"
  var folders = DriveApp.getFoldersByName("Masjid Details");
  if (folders.hasNext()) {
    return folders.next();
  }
  // 2. Try by previously configured Drive folder ID
  try {
    var f = DriveApp.getFolderById("1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n");
    if (f) return f;
  } catch (e) {}
  // 3. Fallback: create "Masjid Details" folder in root
  var newFolder = DriveApp.createFolder("Masjid Details");
  newFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  return newFolder;
}

// Helper: Convert masjid name to clean slug (e.g. "Mohomadiya Masjid" -> "mohomadiya")
function sanitizeSlug(name) {
  if (!name) return "masjid";
  var slug = name.toString().trim().toLowerCase()
    .replace(/masjid/g, "")
    .replace(/[\s\-_]+/g, "_")
    .replace(/[^a-z0-9_]/g, "");
  while (slug.length > 0 && slug.charAt(0) === '_') { slug = slug.substring(1); }
  while (slug.length > 0 && slug.charAt(slug.length - 1) === '_') { slug = slug.substring(0, slug.length - 1); }
  return slug || "masjid";
}

// Run this function ONCE in Google Apps Script editor to grant Drive & Sheet permissions
function myFunction() {
  var file = DriveApp.createFile("test_auth.txt", "Drive Permission Test");
  file.setTrashed(true);
  Logger.log("Drive write permission granted!");
}

function testDrive() {
  myFunction();
}

function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function handleRequest(e) {
  try {
    var p = (e && e.parameter) ? e.parameter : {};
    var action = p.action || "ping";
    
    // 1. Check WebApp connection
    if (action === "ping") {
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Google Apps Script Azan Sync is active!"
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 2. One-click migration of the 4 existing photos + sheet repair via Web App
    if (action === "organizeExisting" || action === "migratePhotos" || action === "fixFormat") {
      var migrationReport = organizeExistingMasjidDetails();
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Migration completed successfully!",
        details: migrationReport
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 3. New Masjid Registration + Subfolder Creation + Photo & QR Upload + 10 Rows
    if (action === "requestMasjid" || action === "requestMasjidToSheet") {
      var name = (p.name || "").toString().trim();
      var loc = (p.location || p.address || "").toString().trim();
      var admin = (p.admin || "").toString().trim();
      var contact = (p.contact || "").toString().trim();
      var photoData = p.photoData || "";
      var qrData = p.qrData || "";
      var photoUrl = "";
      var qrUrl = "";
      
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
      
      // Calculate next sequential 9-digit ID if not provided (e.g. 100111115)
      var finalId = (p.id || "").toString().trim();
      if (!finalId || finalId.length > 10 || isNaN(parseInt(finalId, 10))) {
        var data = sheet.getDataRange().getValues();
        var maxId = 0;
        for (var r = 0; r < data.length; r++) {
          if ((data[r][0] || "").toString().trim().toLowerCase() === "id") {
            var val = parseInt((data[r][1] || "").toString().trim(), 10);
            if (!isNaN(val) && val > maxId) {
              maxId = val;
            }
          }
        }
        var nextNum = (maxId >= 100111111) ? (maxId + 1) : 100111115;
        finalId = nextNum.toString();
      }

      // Root Google Drive folder: "Masjid Details"
      var parentFolder = getMasjidDetailsFolder();
      
      // Subfolder name: <slug>_<id> (e.g. mohomadiya_100111115)
      var slug = sanitizeSlug(name);
      var subfolderName = slug + "_" + finalId;
      
      var masjidFolder;
      var existingFolders = parentFolder.getFoldersByName(subfolderName);
      if (existingFolders.hasNext()) {
        masjidFolder = existingFolders.next();
      } else {
        masjidFolder = parentFolder.createFolder(subfolderName);
      }
      masjidFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      
      // Photo file name: <slug>_photo.jpg (e.g. mohomadiya_photo.jpg)
      var photoFileName = slug + "_photo.jpg";
      // QR file name: <slug>_qr.jpg (e.g. mohomadiya_qr.jpg)
      var qrFileName = slug + "_qr.jpg";
      
      // Save Masjid Photo inside the Masjid subfolder
      if (photoData) {
        try {
          var photoBytes = Utilities.base64Decode(photoData);
          var photoBlob = Utilities.newBlob(photoBytes, "image/jpeg", photoFileName);
          var photoFile = masjidFolder.createFile(photoBlob);
          photoFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          photoUrl = photoFile.getUrl();
        } catch (pe) {
          photoUrl = "Error: " + pe.toString();
        }
      }
      
      // Save Masjid QR code inside the Masjid subfolder
      if (qrData) {
        try {
          var qrBytes = Utilities.base64Decode(qrData);
          var qrBlob = Utilities.newBlob(qrBytes, "image/jpeg", qrFileName);
          var qrFile = masjidFolder.createFile(qrBlob);
          qrFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          qrUrl = qrFile.getUrl();
        } catch (qe) {
          qrUrl = "Error: " + qe.toString();
        }
      }
      
      // 1. Leave exactly one empty row after the last data (Blank row separator)
      var lastRow = sheet.getLastRow();
      var startRow = 1;
      if (lastRow > 0) {
        var lastRowValues = sheet.getRange(lastRow, 1, 1, Math.min(sheet.getLastColumn() || 1, 7)).getValues()[0];
        var isLastRowBlank = true;
        for (var c = 0; c < lastRowValues.length; c++) {
          if (lastRowValues[c] !== "" && lastRowValues[c] !== null && lastRowValues[c] !== undefined) {
            isLastRowBlank = false;
            break;
          }
        }
        startRow = isLastRowBlank ? (lastRow + 1) : (lastRow + 2);
      }
      
      // 2. Prepare exactly 10 rows matching strict format
      var rows = [
        ["Masjid Name", name, "", "", "", "", ""],
        ["Address", loc, "", "", "", "", ""],
        ["ID", finalId, "", "", "", "", ""],
        ["Masjid Photo", photoUrl, "", "", "", "", ""],
        ["Masjid QR", qrUrl, "", "", "", "", ""],
        ["", "Fajr", "Zohar", "Asr", "Maghrib", "Isha", "Jummah"],
        ["Azan", "05:50", "01:15", "05:35", "06:10", "07:50", "12:48"],
        ["Jammat", "06:20", "01:30", "05:45", "06:12", "07:59", "01:30"],
        ["Admin ID", admin || "admin", "", "", "", "", ""],
        ["Password", contact, "", "", "", "", ""]
      ];
      
      // 3. Write all 10 rows to the sheet
      sheet.getRange(startRow, 1, 10, 7).setValues(rows);
      
      // 4. Set Bold & Center styling:
      // Rows 1-5 (Masjid Name, Address, ID, Masjid Photo, Masjid QR) - Column A Bold
      sheet.getRange(startRow, 1, 5, 1).setFontWeight("bold");
      
      // Row 6 (Fajr, Zohar, Asr, Maghrib, Isha, Jummah) - Columns B-G Bold & Centered
      sheet.getRange(startRow + 5, 2, 1, 6).setFontWeight("bold").setHorizontalAlignment("center");
      
      // Rows 7-8 (Azan, Jammat times) - Columns B-G Centered
      sheet.getRange(startRow + 6, 2, 2, 6).setHorizontalAlignment("center");
      
      // Rows 7-10 (Azan, Jammat, Admin ID, Password) - Column A Bold
      sheet.getRange(startRow + 6, 1, 4, 1).setFontWeight("bold");
      
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Masjid " + name + " (#" + finalId + ") added to Sheet1 successfully!",
        folder: subfolderName,
        photoUrl: photoUrl,
        qrUrl: qrUrl
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 4. Update All Timings for a Masjid via Admin Panel
    if (action === "updateAll") {
      var id = (p.id || "").toString().trim();
      if (!id) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Missing id"})).setMimeType(ContentService.MimeType.JSON);
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
      var data = sheet.getDataRange().getValues();
      var foundRow = -1;
      for (var r = 0; r < data.length; r++) {
        if ((data[r][0] || "").toString().trim().toLowerCase() === "id" && (data[r][1] || "").toString().trim() === id) {
          foundRow = r; break;
        }
      }
      if (foundRow === -1) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Masjid ID #" + id + " not found in sheet"})).setMimeType(ContentService.MimeType.JSON);
      var azanRow = -1, jammatRow = -1;
      for (var r = foundRow; r < Math.min(data.length, foundRow + 10); r++) {
        var tag = (data[r][0] || "").toString().trim().toLowerCase();
        if (tag === "azan") azanRow = r;
        if (tag === "jammat") jammatRow = r;
      }
      var prayers = [{key:"fajr",col:1},{key:"zohar",col:2},{key:"asr",col:3},{key:"maghrib",col:4},{key:"isha",col:5},{key:"jumah",col:6}];
      for (var i = 0; i < prayers.length; i++) {
        var item = prayers[i];
        if (azanRow !== -1 && p[item.key + "Azan"]) sheet.getRange(azanRow + 1, item.col + 1).setValue(p[item.key + "Azan"]);
        if (jammatRow !== -1 && p[item.key + "Jammat"]) sheet.getRange(jammatRow + 1, item.col + 1).setValue(p[item.key + "Jammat"]);
      }
      return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Updated timings for #" + id})).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 5. Update Single Prayer Timing
    if (action === "update") {
      var id = (p.id || "").toString().trim();
      var prayer = (p.prayer || "").toString().trim().toLowerCase();
      var azan = (p.azan || "").toString().trim();
      var jammat = (p.jammat || "").toString().trim();
      if (!id || !prayer) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Missing id or prayer"})).setMimeType(ContentService.MimeType.JSON);
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
      var data = sheet.getDataRange().getValues();
      var colMap = {"fajr":1, "zohar":2, "dhuhr":2, "asr":3, "maghrib":4, "isha":5, "jumah":6, "jummah":6, "jum'ah":6};
      var targetCol = colMap[prayer];
      if (targetCol === undefined) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Unknown prayer: " + prayer})).setMimeType(ContentService.MimeType.JSON);
      var foundRow = -1;
      for (var r = 0; r < data.length; r++) {
        if ((data[r][0] || "").toString().trim().toLowerCase() === "id" && (data[r][1] || "").toString().trim() === id) {
          foundRow = r; break;
        }
      }
      if (foundRow === -1) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Masjid ID #" + id + " not found in sheet"})).setMimeType(ContentService.MimeType.JSON);
      var azanRow = -1, jammatRow = -1;
      for (var r = foundRow; r < Math.min(data.length, foundRow + 10); r++) {
        var tag = (data[r][0] || "").toString().trim().toLowerCase();
        if (tag === "azan") azanRow = r;
        if (tag === "jammat") jammatRow = r;
      }
      if (azanRow !== -1 && azan) sheet.getRange(azanRow + 1, targetCol + 1).setValue(azan);
      if (jammatRow !== -1 && jammat) sheet.getRange(jammatRow + 1, targetCol + 1).setValue(jammat);
      return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Updated #" + id + " " + prayer})).setMimeType(ContentService.MimeType.JSON);
    }

    // 6. App Rating
    if (action === "addRating") {
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var rSheet = ss.getSheetByName("rating") || ss.insertSheet("rating");
      var stars = parseFloat(p.stars || "5");
      var lastRow = rSheet.getLastRow();
      var nextId = lastRow <= 1 ? 1 : lastRow;
      rSheet.appendRow([nextId, stars]);
      return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Rating recorded!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // 7. Taqwa Cloud Backup
    if (action === "backupTaqwa") {
      var user = (p.user || "default_user").toString().trim();
      var totalPoints = p.points || "0";
      var restored = p.restored || "0";
      var logsJson = p.logs || "[]";
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var tSheet = ss.getSheetByName("taqwa_backup") || ss.insertSheet("taqwa_backup");
      var data = tSheet.getDataRange().getValues();
      var foundRow = -1;
      for (var r = 0; r < data.length; r++) {
        if ((data[r][0] || "").toString().trim() === user) {
          foundRow = r; break;
        }
      }
      var nowStr = Utilities.formatDate(new Date(), "GMT+5:30", "yyyy-MM-dd HH:mm:ss");
      if (foundRow !== -1) {
        tSheet.getRange(foundRow + 1, 2).setValue(totalPoints);
        tSheet.getRange(foundRow + 1, 3).setValue(restored);
        tSheet.getRange(foundRow + 1, 4).setValue(logsJson);
        tSheet.getRange(foundRow + 1, 5).setValue(nowStr);
      } else {
        if (tSheet.getLastRow() === 0) {
          tSheet.appendRow(["User ID / Phone", "Total Points", "Restored Points", "Logs JSON", "Last Backup"]);
        }
        tSheet.appendRow([user, totalPoints, restored, logsJson, nowStr]);
      }
      return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Taqwa backup saved for " + user})).setMimeType(ContentService.MimeType.JSON);
    }

    // 8. Taqwa Cloud Restore
    if (action === "restoreTaqwa") {
      var user = (p.user || "default_user").toString().trim();
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var tSheet = ss.getSheetByName("taqwa_backup");
      if (!tSheet) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "No backups found"})).setMimeType(ContentService.MimeType.JSON);
      var data = tSheet.getDataRange().getValues();
      var foundRow = -1;
      for (var r = 0; r < data.length; r++) {
        if ((data[r][0] || "").toString().trim() === user) {
          foundRow = r; break;
        }
      }
      if (foundRow === -1) return ContentService.createTextOutput(JSON.stringify({status: "error", message: "No backup found for " + user})).setMimeType(ContentService.MimeType.JSON);
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        user: user,
        totalPoints: parseInt(data[foundRow][1] || "0", 10),
        restoredPoints: parseInt(data[foundRow][2] || "0", 10),
        logsJson: data[foundRow][3] || "[]",
        lastBackup: data[foundRow][4] || ""
      })).setMimeType(ContentService.MimeType.JSON);
    }

    return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Unknown action: " + action})).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()})).setMimeType(ContentService.MimeType.JSON);
  }
}

// ==============================================================================================
// ONE-CLICK MIGRATION FUNCTION:
// 1. Organizes existing photos in "Masjid Details" into folders: <slug>_<id>/<slug>_photo.jpg
// 2. Fixes all rows in Sheet1 to guarantee strict 10 rows format (adds missing Masjid QR / IDs)
// ==============================================================================================
function organizeExistingMasjidDetails() {
  var logs = [];
  var parentFolder = getMasjidDetailsFolder();
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
  
  logs.push("Active Root Folder: " + parentFolder.getName());
  
  // 1. Collect all loose files directly in "Masjid Details"
  var looseFiles = [];
  var fileIter = parentFolder.getFiles();
  while (fileIter.hasNext()) {
    looseFiles.push(fileIter.next());
  }
  logs.push("Found " + looseFiles.length + " files directly in " + parentFolder.getName());
  
  // 2. Read Sheet1 to identify all Masjids
  var data = sheet.getDataRange().getValues();
  var masjids = [];
  var cur = null;
  
  for (var r = 0; r < data.length; r++) {
    var tag = (data[r][0] || "").toString().trim().toLowerCase();
    var val = (data[r][1] || "").toString().trim();
    
    if (tag === "masjid name") {
      if (cur && cur.name) masjids.push(cur);
      cur = {
        name: val,
        address: "",
        id: "",
        idRow: -1,
        photoUrl: "",
        photoRow: -1,
        qrUrl: "",
        qrRow: -1
      };
    } else if (cur) {
      if (tag === "address") {
        cur.address = val;
      } else if (tag === "id") {
        cur.id = val;
        cur.idRow = r + 1;
      } else if (tag === "masjid photo") {
        cur.photoUrl = val;
        cur.photoRow = r + 1;
      } else if (tag === "masjid qr") {
        cur.qrUrl = val;
        cur.qrRow = r + 1;
      }
    }
  }
  if (cur && cur.name) masjids.push(cur);
  logs.push("Found " + masjids.length + " Masjids in Sheet1.");
  
  // Standard fallback IDs and known photo IDs for the 4 primary Masjids
  var defaultMasjids = [
    { name: "Mohammadiya Masjid", id: "100111111", oldPhotoId: "1QP6elu7nlZwmrxjG4p-eyZWOrEaEdiwo" },
    { name: "Hazrat Imam Hussain Masjid", id: "100111112", oldPhotoId: "1Wtt9FYQSWz2o0PpxuCtGJZjSKBOLNhjH" },
    { name: "Abu Bakar Siddique Masjid", id: "100111113", oldPhotoId: "1wtzGogJQgbTLR1rHlTAocBeMdwMESBtD" },
    { name: "Hazrat Jang Bahadur Salabat Kha", id: "100111114", oldPhotoId: "1Ju4p3KkS6sc3vzSvf68Bm1panplC_g8r" }
  ];
  
  var processedFiles = {};
  
  for (var i = 0; i < masjids.length; i++) {
    var m = masjids[i];
    var def = defaultMasjids[i] || {};
    var id = m.id || def.id || (100111111 + i).toString();
    var name = m.name || def.name || ("Masjid_" + id);
    var slug = sanitizeSlug(name);
    var subfolderName = slug + "_" + id;
    
    // Create/get subfolder <slug>_<id> inside "Masjid Details"
    var subFolder;
    var subFolders = parentFolder.getFoldersByName(subfolderName);
    if (subFolders.hasNext()) {
      subFolder = subFolders.next();
    } else {
      subFolder = parentFolder.createFolder(subfolderName);
    }
    subFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    var photoFileName = slug + "_photo.jpg";
    var qrFileName = slug + "_qr.jpg";
    var photoFile = null;
    
    // Try to find photo by URL in sheet
    if (m.photoUrl) {
      var idMatch = m.photoUrl.match(/[-\w]{25,}/);
      if (idMatch) {
        try { photoFile = DriveApp.getFileById(idMatch[0]); } catch (e) {}
      }
    }
    
    // Try by default known file ID
    if (!photoFile && def.oldPhotoId) {
      try { photoFile = DriveApp.getFileById(def.oldPhotoId); } catch (e) {}
    }
    
    // Try matching loose files by name in "Masjid Details"
    if (!photoFile) {
      for (var f = 0; f < looseFiles.length; f++) {
        var lf = looseFiles[f];
        if (lf && !processedFiles[lf.getId()]) {
          var lfName = lf.getName().toLowerCase();
          var slugParts = slug.split("_");
          var matched = false;
          for (var sp = 0; sp < slugParts.length; sp++) {
            if (slugParts[sp].length > 3 && lfName.indexOf(slugParts[sp]) !== -1) {
              matched = true; break;
            }
          }
          if (matched) {
            photoFile = lf; break;
          }
        }
      }
    }
    
    // Fallback by index among remaining loose files
    if (!photoFile) {
      for (var f = 0; f < looseFiles.length; f++) {
        var lf = looseFiles[f];
        if (lf && !processedFiles[lf.getId()]) {
          photoFile = lf; break;
        }
      }
    }
    
    // Move and rename photo
    if (photoFile) {
      try {
        processedFiles[photoFile.getId()] = true;
        photoFile.setName(photoFileName);
        photoFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        try {
          photoFile.moveTo(subFolder);
        } catch (me) {
          subFolder.addFile(photoFile);
          try { parentFolder.removeFile(photoFile); } catch (re) {}
        }
        var newPhotoUrl = photoFile.getUrl();
        if (m.photoRow > 0) {
          sheet.getRange(m.photoRow, 2).setValue(newPhotoUrl);
        }
        logs.push("Moved & renamed: " + photoFile.getName() + " -> " + subfolderName + "/" + photoFileName);
      } catch (err) {
        logs.push("Error moving photo for " + name + ": " + err.toString());
      }
    }
    
    // Move and rename QR if exists
    var qrFile = null;
    if (m.qrUrl) {
      var qrMatch = m.qrUrl.match(/[-\w]{25,}/);
      if (qrMatch) {
        try { qrFile = DriveApp.getFileById(qrMatch[0]); } catch (e) {}
      }
    }
    if (qrFile) {
      try {
        qrFile.setName(qrFileName);
        qrFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        try {
          qrFile.moveTo(subFolder);
        } catch (me2) {
          subFolder.addFile(qrFile);
          try { parentFolder.removeFile(qrFile); } catch (re2) {}
        }
        if (m.qrRow > 0) {
          sheet.getRange(m.qrRow, 2).setValue(qrFile.getUrl());
        }
        logs.push("Moved & renamed QR -> " + subfolderName + "/" + qrFileName);
      } catch (qErr) {
        logs.push("Error moving QR for " + name + ": " + qErr.toString());
      }
    }
  }
  
  // 3. Auto-repair 10 rows format across Sheet1
  repairSheet1TenRows(sheet);
  
  for (var k = 0; k < logs.length; k++) {
    Logger.log(logs[k]);
  }
  return logs;
}

// Helper: Ensures every Masjid in Sheet1 has all 10 rows (inserts missing "Masjid QR" row or ID)
function repairSheet1TenRows(sheet) {
  var data = sheet.getDataRange().getValues();
  for (var r = 0; r < data.length; r++) {
    var tag = (data[r][0] || "").toString().trim().toLowerCase();
    
    // Fix missing ID
    if (tag === "id") {
      var val = (data[r][1] || "").toString().trim();
      if (!val || val === "") {
        var prevId = 100111111;
        for (var p = r - 1; p >= 0; p--) {
          if ((data[p][0] || "").toString().trim().toLowerCase() === "id") {
            var pVal = parseInt(data[p][1], 10);
            if (!isNaN(pVal) && pVal > prevId) prevId = pVal;
          }
        }
        sheet.getRange(r + 1, 2).setValue((prevId + 1).toString());
      }
    }
    
    // Fix missing "Masjid QR" row below "Masjid Photo"
    if (tag === "masjid photo") {
      if (r + 1 < data.length) {
        var nextTag = (data[r + 1][0] || "").toString().trim().toLowerCase();
        if (nextTag !== "masjid qr") {
          sheet.insertRowAfter(r + 1);
          sheet.getRange(r + 2, 1, 1, 7).setValues([["Masjid QR", "", "", "", "", "", ""]]);
          sheet.getRange(r + 2, 1).setFontWeight("bold");
          data = sheet.getDataRange().getValues(); // refresh
        }
      }
    }
  }
}

    """.trimIndent()

    suspend fun updateAllMasjidTimings(
        masjid: MasjidItem,
        webAppUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val targetUrl = webAppUrl?.trim()?.ifBlank { null } ?: APPS_SCRIPT_WEBAPP_URL.trim().ifBlank { null }
        if (targetUrl == null) {
            return@withContext Pair(false, "Apps Script WebApp URL not configured.")
        }
        try {
            val params = mapOf(
                "action" to "updateAll",
                "id" to masjid.id,
                "fajrAzan" to formatForCsv(masjid.fajrAzanFixed ?: "05:40"),
                "fajrJammat" to formatForCsv(masjid.fajrJammatFixed ?: "06:15"),
                "zoharAzan" to formatForCsv(masjid.zoharAzanFixed ?: "13:15"),
                "zoharJammat" to formatForCsv(masjid.zoharJammatFixed ?: "13:30"),
                "asrAzan" to formatForCsv(masjid.asrAzanFixed ?: "17:17"),
                "asrJammat" to formatForCsv(masjid.asrJammatFixed ?: "17:30"),
                "maghribAzan" to formatForCsv(masjid.maghribAzanFixed ?: "18:10"),
                "maghribJammat" to formatForCsv(masjid.maghribJammatFixed ?: "18:12"),
                "ishaAzan" to formatForCsv(masjid.ishaAzanFixed ?: "19:50"),
                "ishaJammat" to formatForCsv(masjid.ishaJammatFixed ?: "19:59"),
                "jumahAzan" to formatForCsv(masjid.jumahAzanTime.ifBlank { "12:30" }),
                "jumahJammat" to formatForCsv(masjid.jumahJammatTime.ifBlank { "13:30" })
            )
            val query = params.entries.joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            }
            var currentUrl = if (targetUrl.contains("?")) "$targetUrl&$query" else "$targetUrl?$query"

            var redirectCount = 0
            var finalCode = 0
            var responseBody = ""

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AzanTimeApp/2.6")
                }
                finalCode = conn.responseCode
                if (finalCode in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }

                responseBody = try {
                    val stream = if (finalCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                break
            }

            if (finalCode in 200..299) {
                if (responseBody.contains("\"error\"") && responseBody.contains("Unknown action")) {
                    Pair(false, "OLD_SCRIPT_FORMAT")
                } else {
                    Pair(true, if (responseBody.isNotBlank()) responseBody.take(120) else "Google Sheet updated successfully")
                }
            } else {
                Pair(false, "Google Sheet sync failed (HTTP $finalCode): ${responseBody.take(100)}")
            }
        } catch (e: Exception) {
            Pair(false, "Sync network error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun updateRemoteGoogleSheet(
        masjidId: String,
        prayerName: String,
        azanTime: String,
        jammatTime: String,
        webAppUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val targetUrl = webAppUrl?.trim()?.ifBlank { null } ?: APPS_SCRIPT_WEBAPP_URL.trim().ifBlank { null }
        if (targetUrl == null) {
            return@withContext Pair(false, "Apps Script WebApp URL not configured. Configure in Admin Panel.")
        }
        try {
            val encodedPrayer = java.net.URLEncoder.encode(prayerName, "UTF-8")
            val encodedAzan = java.net.URLEncoder.encode(formatForCsv(azanTime), "UTF-8")
            val encodedJammat = java.net.URLEncoder.encode(formatForCsv(jammatTime), "UTF-8")
            var currentUrl = if (targetUrl.contains("?")) {
                "$targetUrl&action=update&id=$masjidId&prayer=$encodedPrayer&azan=$encodedAzan&jammat=$encodedJammat"
            } else {
                "$targetUrl?action=update&id=$masjidId&prayer=$encodedPrayer&azan=$encodedAzan&jammat=$encodedJammat"
            }

            var redirectCount = 0
            var finalCode = 0
            var responseBody = ""

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AzanTimeApp/2.6")
                }
                finalCode = conn.responseCode
                if (finalCode in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }

                responseBody = try {
                    val stream = if (finalCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                break
            }

            if (finalCode in 200..299) {
                Pair(true, if (responseBody.isNotBlank()) responseBody.take(80) else "Google Sheet updated successfully")
            } else {
                Pair(false, "Google Sheet sync failed (HTTP $finalCode): ${responseBody.take(100)}")
            }
        } catch (e: Exception) {
            Pair(false, "Sync network error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun testAppsScriptConnection(rawUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return@withContext Pair(false, "URL cannot be empty")
        try {
            var currentUrl = if (clean.contains("?")) "$clean&action=ping" else "$clean?action=ping"
            var redirectCount = 0
            var finalCode = 0
            var responseBody = ""

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AzanTimeApp/2.6")
                }
                finalCode = conn.responseCode
                if (finalCode in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }

                responseBody = try {
                    val stream = if (finalCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                break
            }

            if (finalCode in 200..299) {
                Pair(true, "Connected! Server responded: ${responseBody.take(100)}")
            } else {
                Pair(false, "Server returned HTTP $finalCode: ${responseBody.take(120)}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun fetchCsv(urlStr: String = DEFAULT_SHEET_URL): String = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            reader.readText()
        }
    }

    fun buildCsv(masajid: List<MasjidItem>): String {
        val sb = StringBuilder()
        for (m in masajid) {
            sb.append("Masjid Name,${m.name},,,,,\n")
            sb.append("Address,${m.area},,,,,\n")
            sb.append("ID,${m.id},,,,,\n")
            val photoLink = if (m.photoUrl.isNotBlank()) m.photoUrl else ""
            sb.append("Masjid Photo,$photoLink,,,,,\n")
            val qrLink = if (m.qrUrl.isNotBlank()) m.qrUrl else ""
            sb.append("Masjid QR,$qrLink,,,,,\n")
            sb.append(",Fajr,Zohar,Asr,Maghrib,Isha,Jummah\n")

            val fAzan = formatForCsv(m.fajrAzanFixed ?: "05:40")
            val zAzan = formatForCsv(m.zoharAzanFixed ?: "13:15")
            val aAzan = formatForCsv(m.asrAzanFixed ?: "17:17")
            val mAzan = formatForCsv(m.maghribAzanFixed ?: "18:10")
            val iAzan = formatForCsv(m.ishaAzanFixed ?: "19:50")
            val jAzan = formatForCsv(m.jumahAzanTime)
            sb.append("Azan,$fAzan,$zAzan,$aAzan,$mAzan,$iAzan,$jAzan\n")

            val fJammat = formatForCsv(m.fajrJammatFixed ?: "06:15")
            val zJammat = formatForCsv(m.zoharJammatFixed ?: "13:30")
            val aJammat = formatForCsv(m.asrJammatFixed ?: "17:30")
            val mJammat = formatForCsv(m.maghribJammatFixed ?: "18:12")
            val iJammat = formatForCsv(m.ishaJammatFixed ?: "19:59")
            val jJammat = formatForCsv(m.jumahJammatTime)
            sb.append("Jammat,$fJammat,$zJammat,$aJammat,$mJammat,$iJammat,$jJammat\n")
            val admId = if (m.adminId.isNotBlank()) m.adminId else "admin"
            sb.append("Admin ID,$admId,,,,,\n")
            sb.append("Password,${m.adminPassword},,,,,\n")
            sb.append(",,,,,,\n")
        }
        return sb.toString()
    }

    fun parseCsv(csvText: String): List<MasjidItem> {
        val lines = csvText.lines()
        val list = mutableListOf<MasjidItem>()

        var currentName = ""
        var currentAddress = ""
        var currentId = ""
        var currentPhoto = ""
        var currentQr = ""
        var currentAdminId = "admin"
        var currentPassword = ""
        var azanTimes = listOf<String>()
        var jammatTimes = listOf<String>()

        fun flushCurrent() {
            if (currentName.isNotBlank()) {
                val finalId = if (currentId.isNotBlank()) {
                    currentId.trim()
                } else {
                    // Generate stable 9-digit numeric ID if admin hasn't filled ID column yet
                    val num = Math.abs(currentName.trim().hashCode() % 900000 + 100000)
                    "100$num"
                }

                // Azan: Fajr (AM), Zohar (PM), Asr (PM), Maghrib (PM), Isha (PM), Jummah (12:30 PM)
                val fAzan = azanTimes.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, false) } ?: "05:40"
                val zAzan = azanTimes.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "13:15"
                val aAzan = azanTimes.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "17:17"
                val mAzan = azanTimes.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "18:10"
                val iAzan = azanTimes.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "19:50"
                val jAzan = azanTimes.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "12:30"

                // Jammat: Fajr (AM), Zohar (PM), Asr (PM), Maghrib (PM), Isha (PM), Jummah (PM)
                val fJammat = jammatTimes.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, false) } ?: "06:15"
                val zJammat = jammatTimes.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "13:30"
                val aJammat = jammatTimes.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "17:30"
                val mJammat = jammatTimes.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "18:12"
                val iJammat = jammatTimes.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "19:59"
                val jJammat = jammatTimes.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() }?.let { to24Hr(it, true) } ?: "13:30"

                val directPhoto = extractGoogleDriveDirectUrl(currentPhoto)
                val directQr = extractGoogleDriveDirectUrl(currentQr)

                val masjidItem = MasjidItem(
                    id = finalId,
                    name = currentName.trim(),
                    area = currentAddress.trim().ifBlank { "Solapur" },
                    city = "Solapur",
                    state = "Maharashtra",
                    photoUrl = directPhoto,
                    qrUrl = directQr,
                    jumahAzanTime = jAzan,
                    jumahJammatTime = jJammat,
                    fajrAzanFixed = fAzan,
                    fajrJammatFixed = fJammat,
                    zoharAzanFixed = zAzan,
                    zoharJammatFixed = zJammat,
                    asrAzanFixed = aAzan,
                    asrJammatFixed = aJammat,
                    maghribAzanFixed = mAzan,
                    maghribJammatFixed = mJammat,
                    ishaAzanFixed = iAzan,
                    ishaJammatFixed = iJammat,
                    adminId = currentAdminId.ifBlank { "admin" },
                    adminPassword = currentPassword
                )
                list.add(masjidItem)
            }
            currentName = ""
            currentAddress = ""
            currentId = ""
            currentPhoto = ""
            currentQr = ""
            currentAdminId = "admin"
            currentPassword = ""
            azanTimes = emptyList()
            jammatTimes = emptyList()
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val tokens = splitCsvTokens(line)
            if (tokens.isEmpty()) continue

            val first = tokens[0].trim().lowercase(java.util.Locale.ROOT)
            if (first.contains("masjid name")) {
                if (currentName.isNotBlank()) {
                    flushCurrent()
                }
                currentName = tokens.getOrElse(1) { "" }.trim()
            } else if (first == "address" || first.contains("location")) {
                currentAddress = tokens.getOrElse(1) { "" }.trim()
            } else if (first == "id") {
                currentId = tokens.getOrElse(1) { "" }.trim()
            } else if (first.contains("masjid photo") || first == "photo") {
                currentPhoto = tokens.getOrElse(1) { "" }.trim()
                // Support inline QR if formatted as [Masjid Photo, photoUrl, Masjid QR, qrUrl]
                if (tokens.size >= 4 && tokens[2].trim().lowercase(java.util.Locale.ROOT).contains("qr")) {
                    currentQr = tokens.getOrElse(3) { "" }.trim()
                }
            } else if (first.contains("masjid qr") || first == "qr" || first.contains("qr code")) {
                currentQr = tokens.getOrElse(1) { "" }.trim()
            } else if (first == "azan") {
                azanTimes = tokens.drop(1).map { it.trim() }
            } else if (first == "jammat") {
                jammatTimes = tokens.drop(1).map { it.trim() }
            } else if (first.contains("admin")) {
                currentAdminId = tokens.getOrElse(1) { "admin" }.trim()
            } else if (first == "password") {
                currentPassword = tokens.getOrElse(1) { "" }.trim()
            }
        }
        flushCurrent()
        return list
    }

    private fun splitCsvTokens(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        for (c in line) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim().removeSurrounding("\"").trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString().trim().removeSurrounding("\"").trim())
        return result
    }

    const val RATING_SHEET_URL = "https://docs.google.com/spreadsheets/d/13l1dJh64fyOnpHFlw81iWKHZlA5ko0JVWJ_qoF43k0g/gviz/tq?tqx=out:csv&sheet=rating"

    suspend fun fetchRatingStats(): Pair<Double, Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL(RATING_SHEET_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "AzanTimeApp/2.6")
            }
            if (conn.responseCode == 200) {
                val csv = conn.inputStream.bufferedReader().use { it.readText() }
                val lines = csv.lines().map { it.trim().trim('"') }.filter { it.isNotBlank() }
                val starsList = mutableListOf<Double>()
                for (line in lines.drop(1)) {
                    val parts = line.split(",").map { it.trim().trim('"') }
                    if (parts.size >= 2) {
                        val s = parts[1].toDoubleOrNull() ?: parts[0].toDoubleOrNull()
                        if (s != null && s in 0.5..5.0) starsList.add(s)
                    } else if (parts.size == 1) {
                        val s = parts[0].toDoubleOrNull()
                        if (s != null && s in 0.5..5.0) starsList.add(s)
                    }
                }
                if (starsList.isNotEmpty()) {
                    val avg = starsList.average()
                    return@withContext Pair(avg, starsList.size)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        Pair(0.0, 0)
    }

    suspend fun submitRating(stars: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val starsStr = if (stars % 1.0 == 0.0) stars.toInt().toString() else String.format(Locale.US, "%.1f", stars)
            val query = "action=addRating&stars=$starsStr"
            val targetUrl = if (APPS_SCRIPT_WEBAPP_URL.contains("?")) "$APPS_SCRIPT_WEBAPP_URL&$query" else "$APPS_SCRIPT_WEBAPP_URL?$query"
            val url = URL(targetUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
            }
            conn.responseCode in 200..399
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitRating(stars: Int): Boolean = submitRating(stars.toDouble())

    suspend fun submitMasjidRequestAuto(
        name: String,
        location: String,
        adminName: String,
        contact: String,
        photoBase64: String? = null,
        photoName: String? = null,
        qrBase64: String? = null,
        qrName: String? = null,
        webAppUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = webAppUrl?.trim()?.ifBlank { null } ?: APPS_SCRIPT_WEBAPP_URL.trim()
            val postData = StringBuilder()
            postData.append("action=").append(java.net.URLEncoder.encode("requestMasjidToSheet", "UTF-8"))
            postData.append("&id=").append(System.currentTimeMillis())
            postData.append("&name=").append(java.net.URLEncoder.encode(name, "UTF-8"))
            postData.append("&location=").append(java.net.URLEncoder.encode(location, "UTF-8"))
            postData.append("&admin=").append(java.net.URLEncoder.encode(adminName, "UTF-8"))
            postData.append("&contact=").append(java.net.URLEncoder.encode(contact, "UTF-8"))
            if (!photoBase64.isNullOrBlank()) {
                postData.append("&photoData=").append(java.net.URLEncoder.encode(photoBase64, "UTF-8"))
                postData.append("&photoName=").append(java.net.URLEncoder.encode(photoName ?: "$name.jpg", "UTF-8"))
            }
            if (!qrBase64.isNullOrBlank()) {
                postData.append("&qrData=").append(java.net.URLEncoder.encode(qrBase64, "UTF-8"))
                postData.append("&qrName=").append(java.net.URLEncoder.encode(qrName ?: "${name}_QR.jpg", "UTF-8"))
            }
            val postBytes = postData.toString().toByteArray(Charsets.UTF_8)

            var conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 20000
                readTimeout = 20000
            }
            conn.outputStream.use { os ->
                os.write(postBytes)
            }

            var responseCode = conn.responseCode
            // Follow 302 redirect from Google Apps Script
            if (responseCode in listOf(301, 302, 303, 307, 308)) {
                val redirectUrl = conn.getHeaderField("Location")
                if (!redirectUrl.isNullOrBlank()) {
                    conn.disconnect()
                    conn = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 20000
                        readTimeout = 20000
                    }
                    responseCode = conn.responseCode
                }
            }

            val responseText = if (responseCode in 200..399) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            }
            conn.disconnect()

            try {
                val json = org.json.JSONObject(responseText)
                if (json.optString("status") == "success") {
                    Pair(true, json.optString("message", "Masjid $name saved to Sheet1 successfully!"))
                } else {
                    val err = json.optString("message", "Submission error")
                    Pair(false, err)
                }
            } catch (e: Exception) {
                if (responseCode in 200..399) {
                    Pair(true, "Submitted successfully!")
                } else {
                    Pair(false, "Server responded with HTTP $responseCode: $responseText")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Connection failed. Please check internet connection.")
        }
    }

    suspend fun backupTaqwaToCloud(
        userId: String,
        totalPoints: Int,
        restoredPoints: Int,
        logsJson: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(APPS_SCRIPT_WEBAPP_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
            }
            val postData = StringBuilder()
            postData.append("action=").append(java.net.URLEncoder.encode("backupTaqwa", "UTF-8"))
            postData.append("&user=").append(java.net.URLEncoder.encode(userId, "UTF-8"))
            postData.append("&points=").append(totalPoints)
            postData.append("&restored=").append(restoredPoints)
            postData.append("&logs=").append(java.net.URLEncoder.encode(logsJson, "UTF-8"))
            conn.outputStream.use { os ->
                os.write(postData.toString().toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode in 200..399) {
                Pair(true, "Cloud Sync Successful!")
            } else {
                Pair(false, "Server returned code ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Sync failed")
        }
    }

    suspend fun restoreTaqwaFromCloud(
        userId: String
    ): Triple<Int, Int, String>? = withContext(Dispatchers.IO) {
        try {
            val q = "action=restoreTaqwa&user=${java.net.URLEncoder.encode(userId, "UTF-8")}"
            val targetUrl = if (APPS_SCRIPT_WEBAPP_URL.contains("?")) "$APPS_SCRIPT_WEBAPP_URL&$q" else "$APPS_SCRIPT_WEBAPP_URL?$q"
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
            }
            if (conn.responseCode in 200..399) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(responseText)
                if (json.optString("status") == "success") {
                    val pts = json.optInt("totalPoints", 0)
                    val rest = json.optInt("restoredPoints", 0)
                    val logs = json.optString("logsJson", "[]")
                    return@withContext Triple(pts, rest, logs)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        null
    }
}
