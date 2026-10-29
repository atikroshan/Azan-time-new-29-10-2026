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
      
      // Check if this is Guest Masjid
      var isGuestReq = (name.toLowerCase().indexOf("guest") !== -1 || finalId.toLowerCase() === "guest");
      var slug = sanitizeSlug(name);
      var subfolderName = isGuestReq ? "Guest Masjid_Guest" : (slug + "_" + finalId);
      
      // If folder "guest_Guest" exists in Drive, rename it to "Guest Masjid_Guest"
      var oldGReq = parentFolder.getFoldersByName("guest_Guest");
      while (oldGReq.hasNext()) {
        try { oldGReq.next().setName("Guest Masjid_Guest"); } catch (ge) {}
      }
      
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
      
      // Save Masjid Photo inside the Masjid subfolder (Skip if Guest)
      if (photoData && !isGuestReq) {
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
      
      // Save Masjid QR code inside the Masjid subfolder (Skip if Guest)
      if (qrData && !isGuestReq) {
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
    
    // Check if this is Guest Masjid
    var isGuest = (name.toLowerCase().indexOf("guest") !== -1 || id.toLowerCase() === "guest");
    var slug = sanitizeSlug(name);
    var subfolderName = isGuest ? "Guest Masjid_Guest" : (slug + "_" + id);
    
    // Rename any existing "guest_Guest" folder to "Guest Masjid_Guest"
    if (isGuest) {
      var oldGFolders = parentFolder.getFoldersByName("guest_Guest");
      while (oldGFolders.hasNext()) {
        try { oldGFolders.next().setName("Guest Masjid_Guest"); } catch (e) {}
      }
    }
    
    // Create/get subfolder inside "Masjid Details"
    var subFolder;
    var subFolders = parentFolder.getFoldersByName(subfolderName);
    if (subFolders.hasNext()) {
      subFolder = subFolders.next();
    } else {
      subFolder = parentFolder.createFolder(subfolderName);
    }
    subFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    // STRICT GUEST HANDLER: Guest Masjid has NO photo and NO QR.
    if (isGuest) {
      if (m.photoRow > 0) {
        sheet.getRange(m.photoRow, 2).setValue("");
      }
      if (m.qrRow > 0) {
        sheet.getRange(m.qrRow, 2).setValue("");
      }
      logs.push("Guest Masjid handled: Folder set to 'Guest Masjid_Guest', photo and QR links cleared in Sheet1.");
      continue; // Skip photo & QR file assignment for Guest Masjid
    }
    
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
