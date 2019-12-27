# cordova-zebra-print
A Cordova driver for Zebra printers

Plugin based of: https://github.com/michael79bxl/zbtprinter

Example:

Printer ZPL:
```
cordova.plugins.zbtprinter.print("AC:3F:A4:52:73:C4","^XA^FO10,10^AFN,26,13^FDHello, World!^FS^XZ",
    (success) => { 
        alert("Print ok"); 
    }, function(fail) => { 
        alert(fail); 
    }
);
```
List Linked Devices:
```
window.cordova.plugins.zbtprinter.find(
  (data) => {
    alert(JSON.stringify(data)); 
    console.log(data);
  },
  (fail) => {
    alert(fail);
  }
);
```

Install Cordova

```
cordova plugin add https://github.com/kevbarrios/cordova-zebra-print.git
```
ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
