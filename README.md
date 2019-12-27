# zbtprinter
A Cordova driver for Zebra printers

##Usage
You can send data in ZPL Zebra Programing Language:

```
cordova.plugins.zbtprinter.print("AC:3F:A4:52:73:C4","^XA^FO10,10^AFN,26,13^FDHello, World!^FS^XZ",
    function(success) { 
        alert("Print ok"); 
    }, function(fail) { 
        alert(fail); 
    }
);

window.cordova.plugins.zbtprinter.find(
  (data) => {
    alert(data); 
  },
  (fail) => {
    alert(fail);
  }
);
```

##Install
###Cordova

```
cordova plugin add https://github.com/mmilidoni/zbtprinter.git
```

##ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
