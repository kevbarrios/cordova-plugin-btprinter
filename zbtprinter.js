var exec = require("cordova/exec");
 
  function ZebraBluetoothPrinter() {}

  ZebraBluetoothPrinter.prototype.esegui = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'print', ["ciccio", "AC:3F:A4:52:73:C4"]);
  };
  var bluetoothPrinter = new ZebraBluetoothPrinter();
  module.exports = bluetoothPrinter;

