' Open CAD object from filtered EBOM inside CATIA V5 
' The program reads a list of objects that are read Start CATIA if not already started
' Silently open the data
'
' ---- SCRIPT CUSTOMIZATION ----
'
Dim Debug
Debug = false
Dim DisplayMessage
DisplayMessage = false

If DisplayMessage = true then WScript.Echo "Partial Open From 3DEXPERIENCE Version8"

Dim CATIA
Set CATIA = Nothing

' Get CATIA Application
On Error Resume Next
'WScript.Echo "Calling: GetObject(CATIA.Application)"
Set CATIA = GetObject(, "CATIA.Application")

If (Err) Or (CATIA Is Nothing) Then
    If DisplayMessage = true then WScript.Echo "CATIA Not Started -> exiting"
	Msgbox "CATIA Not Started"
    WScript.Quit
End If
On Error GoTo 0

'If Debug = True then Msgbox "End check CATIA OK"
' ---- 3DEXPERIENCE Connection ----

' Get V6Engine
Dim ConnectionStatus
ConnectionStatus = false

Dim V6Engine
Dim CD5Engine

On Error Resume Next
Set V6Engine = CATIA.GetItem("V6Engine")

If (Err) Or (V6Engine Is Nothing) Then
    Set CD5Engine = CATIA.GetItem("CD5Engine")
	ConnectionStatus = CD5Engine.IsConnected
Else
	ConnectionStatus = V6Engine.IsConnected
End If

On Error GoTo 0

' Connect to 3DEXPERIENCE if needed
If ConnectionStatus Then
    If DisplayMessage = true then WScript.Echo "CATIA is connected to 3DEXPERIENCE"
Else
    If DisplayMessage = true then WScript.Echo "CATIA is not connected to 3DEXPERIENCE"
	Msgbox "CATIA is not connected to 3DEXPERIENCE"
    WScript.Quit
    'WScript.Echo "Calling: V6Engine.Connect"
    'CD5Engine.Connect ENOVIAUserLogin,ENOVIAUserPassword,ENOVIASecurityContext
End If

'Location of the File CATVBA
'Folder of vba location"
On Error Resume Next
Dim WSHShell
Set WSHShell = CreateObject("WScript.Shell")
LibPath = WSHShell.RegRead("HKEY_CURRENT_USER\OpenCADFromFilteredEBOM\CATVBAPath")

'If Debug = True then Msgbox "End Check 3DEXPERIENCE Connection"

Dim MyArguments(0)
MyArguments(0) = WScript.Arguments(0)

If Debug = True then Msgbox "Launching vba"
'OpenCADObjectsFromFilteredEBOM
'LibPath = StrPath & "OpenCADFromFilteredEBOM.catvba"
sModule = "OpenCADFromFilteredBOM"
sMacro =  "OpenCADFromFilteredEBOM"

If DisplayMessage = true then WScript.Echo LibPath & " is running ..."
Call CATIA.SystemService.ExecuteScript (LibPath, 2, sModule, sMacro, MyArguments)

If Err Then
    ErrorText = Err.Description
    Msgbox "Error: " & ErrorText
    On Error GoTo 0
End If

If Debug = True then Msgbox "End vbs"

WScript.Quit