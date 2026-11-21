Unicode true
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"

!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\Forge"

Name "Forge"
OutFile "${OUTFILE}"
; LZMA saves 14 MB but takes nearly twice as long to install: most of the payload is already compressed.
SetCompressor /SOLID zlib
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\Forge"
InstallDirRegKey HKCU "Software\Forge" "InstallLocation"

!define MUI_ICON "${ICON}"
!define MUI_UNICON "${ICON}"

Var DeleteOld

!define MUI_COMPONENTSPAGE_NODESC
!insertmacro MUI_PAGE_COMPONENTS
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE DirLeave
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_RUN "$INSTDIR\forge.exe"
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

; Only an empty or existing Forge folder, so delete and uninstall never touch other files.
Function CheckFolder
  ${IfNot} ${FileExists} "$INSTDIR\forge.exe"
    ${DirState} "$INSTDIR" $0
    ${If} $0 == 1
      MessageBox MB_OK|MB_ICONEXCLAMATION "$INSTDIR$\r$\nalready contains other files.$\r$\n$\r$\nChoose an empty folder or an existing Forge folder." /SD IDOK
      Abort
    ${EndIf}
  ${EndIf}
FunctionEnd

Function DirLeave
  StrCpy $DeleteOld 0
  Call CheckFolder

  ; Without admin rights a Program Files install would fail halfway through the copy.
  ClearErrors
  CreateDirectory "$INSTDIR"
  FileOpen $0 "$INSTDIR\.forge-write-test" w
  ${If} ${Errors}
    MessageBox MB_OK|MB_ICONEXCLAMATION "The installer cannot write to$\r$\n$INSTDIR$\r$\n$\r$\nChoose a folder you can write to, such as one inside your user folder."
    Abort
  ${EndIf}
  FileClose $0
  Delete "$INSTDIR\.forge-write-test"

  ${IfNot} ${FileExists} "$INSTDIR\forge.exe"
    Return
  ${EndIf}
  ; A profile file can point Forge's data into this folder.
  ${If} ${FileExists} "$INSTDIR\forge.profile.properties"
    Return
  ${EndIf}
  ; A task dialog shows the question as a heading, which a message box cannot. 14 = Yes, No and Cancel buttons; it returns 6, 7 or 2.
  StrCpy $0 7
  System::Call `comctl32::TaskDialog(p $HWNDPARENT, p 0, w "Forge Setup", w "Delete the old version before installing? (Recommended)", w "Forge is already installed in$\n$INSTDIR$\n$\nThis avoids compatibility problems between old and new files.$\n$\nYour user data is kept either way:$\n- Decks, preferences and saved games in$\n  $APPDATA\Forge$\n- Downloaded card pictures in$\n  $LOCALAPPDATA\Forge\Cache$\n- Custom skins in this folder's res\skins", i 14, p 0, *i .r0)`
  ${If} $0 == 2
    Abort
  ${EndIf}
  ${If} $0 == 6
    ${If} ${FileExists} "$INSTDIR.old\*.*"
      MessageBox MB_OK|MB_ICONEXCLAMATION "Remove or rename $INSTDIR.old first."
      Abort
    ${EndIf}
    StrCpy $DeleteOld 1
  ${EndIf}
FunctionEnd

Section "Forge"
  SectionIn RO
  ; A silent install shows no pages, so the folder page's check never ran.
  Call CheckFolder

  ${If} $DeleteOld == 1
    ; Rename fails while Forge has files open, leaving the old install untouched.
    ClearErrors
    Rename "$INSTDIR" "$INSTDIR.old"
    ${If} ${Errors}
      MessageBox MB_OK|MB_ICONSTOP "The old installation could not be moved. Close Forge and run the installer again. If Forge is not running, choose a folder that does not need administrator rights."
      Abort
    ${EndIf}
    CreateDirectory "$INSTDIR\res"
    Rename "$INSTDIR.old\res\skins" "$INSTDIR\res\skins"
    DetailPrint "Removing the old installation..."
    ; RMDir /r deletes through junctions into other folders; cmd's rmdir removes only the link.
    nsExec::Exec 'cmd /c rmdir /s /q "$INSTDIR.old"'
    Pop $0
  ${EndIf}

  ; Jar names carry the version, so an install over an older one would keep the old jars.
  Delete "$INSTDIR\forge-gui-*-jar-with-dependencies.jar"
  SetOutPath "$INSTDIR"
  File /r "${DIST}/*"

  WriteUninstaller "$INSTDIR\uninstall.exe"
  WriteRegStr HKCU "Software\Forge" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayName" "Forge"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "${UNINST_KEY}" "Publisher" "Forge Developers"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\forge.exe"
  WriteRegStr HKCU "${UNINST_KEY}" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKCU "${UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKCU "${UNINST_KEY}" "NoRepair" 1
SectionEnd

Section "Start menu shortcuts"
  CreateDirectory "$SMPROGRAMS\Forge"
  CreateShortcut "$SMPROGRAMS\Forge\Forge.lnk" "$INSTDIR\forge.exe"
  CreateShortcut "$SMPROGRAMS\Forge\Forge Adventure.lnk" "$INSTDIR\forge-adventure.exe"
SectionEnd

Section "Desktop shortcut"
  CreateShortcut "$DESKTOP\Forge.lnk" "$INSTDIR\forge.exe"
SectionEnd

; Removes only what the installer shipped; any other files keep the folder.
Section "Uninstall"
  Delete "$DESKTOP\Forge.lnk"
  RMDir /r "$SMPROGRAMS\Forge"
  !include "${UNINSTALL_LIST}"
  Delete "$INSTDIR\forge-gui-*-jar-with-dependencies.jar"
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"
  DeleteRegKey HKCU "${UNINST_KEY}"
  DeleteRegKey HKCU "Software\Forge"
SectionEnd
