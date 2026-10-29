// ==============================================================================================
// AZAN TIME SOLAPUR - GOOGLE APPS SCRIPT (FULL BACKEND SCRIPT)
// Google Drive Root Folder ID: 1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n
// Features:
// 1. Automatically creates a folder named after the Masjid inside the main Drive folder
// 2. Saves both Masjid Photo AND Masjid QR Code inside that Masjid folder
// 3. Appends 10 formatted rows into Sheet1 (Masjid QR row right under Masjid Photo)
// 4. Update timings (updateAll & update), ratings, Taqwa backup & restore
// ==============================================================================================

// Run this function ONCE in Google Apps Script editor to grant Drive write permissions
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
    
    // 2. New Masjid Registration + Subfolder Creation + Photo & QR Upload + 10 Rows
    if (action === "requestMasjid" || action === "requestMasjidToSheet") {
      var name = (p.name || "").toString().trim();
      var loc = (p.location || p.address || "").toString().trim();
      var admin = (p.admin || "").toString().trim();
      var contact = (p.contact || "").toString().trim();
      var photoData = p.photoData || "";
      var photoName = (p.photoName || (name + ".jpg")).toString().trim();
      var qrData = p.qrData || "";
      var qrName = (p.qrName || (name + "_QR.jpg")).toString().trim();
      var photoUrl = "";
      var qrUrl = "";
      
      // Main Google Drive folder ID
      var rootFolderId = "1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n";
      var parentFolder;
      try {
        parentFolder = DriveApp.getFolderById(rootFolderId);
      } catch (fe) {
        parentFolder = DriveApp.getRootFolder();
      }
      
      // Create or get subfolder named after the Masjid inside the root folder
      var masjidFolder;
      var folderName = name || "New_Masjid";
      var existingFolders = parentFolder.getFoldersByName(folderName);
      if (existingFolders.hasNext()) {
        masjidFolder = existingFolders.next();
      } else {
        masjidFolder = parentFolder.createFolder(folderName);
      }
      masjidFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      
      // Save Masjid Photo inside the Masjid subfolder
      if (photoData) {
        try {
          var photoBytes = Utilities.base64Decode(photoData);
          var photoBlob = Utilities.newBlob(photoBytes, "image/jpeg", photoName);
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
          var qrBlob = Utilities.newBlob(qrBytes, "image/jpeg", qrName);
          var qrFile = masjidFolder.createFile(qrBlob);
          qrFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          qrUrl = qrFile.getUrl();
        } catch (qe) {
          qrUrl = "Error: " + qe.toString();
        }
      }
      
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
      
      // 1. Leave exactly one empty row after the last data (Blank row separator)
      var lastRow = sheet.getLastRow();
      var startRow = lastRow > 0 ? (lastRow + 2) : 1;
      
      // 2. Prepare 10 rows matching exact format
      var rows = [
        ["Masjid Name", name, "", "", "", "", ""],
        ["Address", loc, "", "", "", "", ""],
        ["ID", "", "", "", "", "", ""],
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
      
      // 4. Set Bold & Center styling exactly matching Admin_Masjid_Names:
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
        message: "Masjid " + name + " added to Sheet1 successfully!",
        photoUrl: photoUrl,
        qrUrl: qrUrl
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 3. Update All Timings for a Masjid via Admin Panel
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
    
    // 4. Update Single Prayer Timing
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

    // 5. App Rating
    if (action === "addRating") {
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var rSheet = ss.getSheetByName("rating") || ss.insertSheet("rating");
      var stars = parseFloat(p.stars || "5");
      var lastRow = rSheet.getLastRow();
      var nextId = lastRow <= 1 ? 1 : lastRow;
      rSheet.appendRow([nextId, stars]);
      return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Rating recorded!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // 6. Taqwa Cloud Backup
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

    // 7. Taqwa Cloud Restore
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
