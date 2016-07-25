var exec = require('cordova/exec');

function andrewBLE() { 
 console.log("andrewBLE has been created");
}

andrewBLE.prototype.startScan = function(success, fail){
 console.log("andrewBLE.js: Starting Scann for Bluetooth LE.");

 exec(success, fail, "andrewBLE", "startScan",[]);
}

andrewBLE.prototype.stopScan = function(success, fail){
 console.log("andrewBLE.js: Stopping Scan for Bluetooth LE.");

 exec(success, fail, "andrewBLE", "stopScan",[]);
}

andrewBLE.prototype.disconnect = function(success, fail){
 console.log("andrewBLE.js: Disconnecting from Bluetooth LE.");

 exec(success, fail, "andrewBLE", "disconnect",[]);
}

andrewBLE.prototype.onButtonPressed = function(success, fail){
 console.log("andrewBLE.js: The button has been pressed!!!");

 exec(success, fail, "andrewBLE", "onButtonPressed",[]);
}

andrewBLE.prototype.onConnectionStateChange = function(success, fail){
 console.warn("andrewBLE.js: The Bluetooth is Disconnected!");

 exec(success, fail, "andrewBLE", "onConnectionStateChange",[]);
}

 var p = new andrewBLE();
 module.exports = p;
