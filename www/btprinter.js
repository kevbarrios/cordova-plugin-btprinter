var exec = require('cordova/exec');

exports.find = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'find', []);
};

exports.print = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'printZPL', [mac, str]);
};

exports.print = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'printText', [mac, str]);
};

exports.printImage = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'printImage', [mac, str]);
};
