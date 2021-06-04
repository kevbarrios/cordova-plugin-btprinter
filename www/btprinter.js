var exec = require('cordova/exec');

exports.find = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BluetoothPrinter', 'find', []);
};

exports.printZPL = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BluetoothPrinter', 'printZPL', [mac, str]);
};

exports.printText = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BluetoothPrinter', 'printText', [mac, str]);
};

exports.printImage = function(mac, str, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'BluetoothPrinter', 'printImage', [mac, str]);
};
