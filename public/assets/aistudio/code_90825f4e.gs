// 1. Ye function Google Drive ki Create/Write permission activate karega
function myFunction() {
  var file = DriveApp.createFile("test_auth.txt", "Drive Permission Test");
  file.setTrashed(true); // Turant delete ho jayega
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
    
    // Test connection check
    if (action === "ping") {
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Google Apps Script Azan Sync is active!"
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // New Masjid Registration + Photo Upload + 9 Rows Formatting
    if (action === "requestMasjid" || action === "requestMasjidToSheet") {
      var name = (p.name || "").toString().trim();
      var loc = (p.location || p.address || "").toString().trim();
      var admin = (p.admin || "").toString().trim();
      var contact = (p.contact || "").toString().trim();
      var photoData = p.photoData || "";
      var photoName = (p.photoName || (name + ".jpg")).toString().trim();
      var photoUrl = "";
      
      // Save Photo to Google Drive
      if (photoData) {
        try {
          var bytes = Utilities.base64Decode(photoData);
          var blob = Utilities.newBlob(bytes, "image/jpeg", photoName);
          var folder;
          try {
            folder = DriveApp.getFolderById("1zEbq9A1LvGk-etUfVcMHUhKd6bqVH01n");
          } catch(fe) {
            folder = DriveApp.getRootFolder();
          }
          var file = folder.createFile(blob);
          file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
          photoUrl = file.getUrl();
        } catch (e) {
          photoUrl = "Error: " + e.toString();
        }
      }
      
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Sheet1") || ss.getActiveSheet();
      
      // 1. Ek poori row empty chhodna (Blank row separation)
      var lastRow = sheet.getLastRow();
      var startRow = lastRow > 0 ? (lastRow + 2) : 1;
      
      // 2. Exact 9 rows ka data block
      var rows = [
        ["Masjid Name", name, "", "", "", "", ""],
        ["Address", loc, "", "", "", "", ""],
        ["ID", "", "", "", "", "", ""],
        ["Masjid Photo", photoUrl, "", "", "", "", ""],
        ["", "Fajr", "Zohar", "Asr", "Maghrib", "Isha", "Jummah"],
        ["Azan", "05:50", "01:15", "05:35", "06:10", "07:50", "12:48"],
        ["Jammat", "06:20", "01:30", "05:45", "06:12", "07:59", "01:30"],
        ["Admin ID", admin || "admin", "", "", "", "", ""],
        ["Password", contact, "", "", "", "", ""]
      ];
      
      // 3. Write rows to sheet
      sheet.getRange(startRow, 1, 9, 7).setValues(rows);
      
      // 4. Styling (Bold and Center exact like screenshot)
      sheet.getRange(startRow, 1, 4, 1).setFontWeight("bold");
      sheet.getRange(startRow + 4, 2, 1, 6).setFontWeight("bold").setHorizontalAlignment("center");
      sheet.getRange(startRow + 5, 2, 2, 6).setHorizontalAlignment("center");
      sheet.getRange(startRow + 5, 1, 4, 1).setFontWeight("bold");
      
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Masjid " + name + " added to Sheet1 successfully!",
        photoUrl: photoUrl
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // Update Timings via Admin Panel
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
      for (var r = foundRow; r < Math.min(data.length, foundRow + 8); r++) {
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
    
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Unknown action: " + action})).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()})).setMimeType(ContentService.MimeType.JSON);
  }
}