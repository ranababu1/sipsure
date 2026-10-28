# SipSure keeps no reflection-based code of its own. The receivers, the device-admin receiver and
# the FileProvider are declared in the manifest, which R8 keeps automatically.
# Keep line numbers so release crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
