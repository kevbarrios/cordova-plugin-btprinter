# cordova-plugin-btprinter
A Cordova driver for printers

Plugin based of: https://github.com/michael79bxl/zbtprinter

Example:

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

Printer ZPL:
```
cordova.plugins.zbtprinter.printZPL("AC:3F:A4:52:73:C4","^XA^FO10,10^AFN,26,13^FDHello, World!^FS^XZ",
    (success) => { 
        console.log("Print ZPL ok"); 
    }, function(fail) => { 
        console.log(fail); 
    }
);
```

Printer Text:
```
cordova.plugins.zbtprinter.printText("AC:3F:A4:52:73:C4","Hello, World!",
    (success) => { 
        console.log("Print Text ok"); 
    }, function(fail) => { 
        console.log(fail); 
    }
);
```

Printer Image:
```
cordova.plugins.zbtprinter.printText("AC:3F:A4:52:73:C4","data:image/jpg;base64, ....",
    (success) => { 
        console.log("Print Image ok"); 
    }, function(fail) => { 
        console.log(fail); 
    }
);
```

Install Cordova

```
cordova plugin add https://github.com/kevbarrios/cordova-plugin-zebra_printer_zpl.git

cordova plugin add cordova-plugin-zebra_printer_zpl
```

config.xml

```
<plugin name="com.github.kevbarrios.zbtprinter" spec="cordova-plugin-zebra_printer_zpl@^1.0.5" />
```

ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
