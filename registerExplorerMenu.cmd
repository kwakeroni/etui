@echo off
setlocal
if not "%1" == "" (
    if /I not "%1" == "Top" (
        if /I not "%1" == "Bottom" (
            echo Unexpected argument "%1"
            echo.
            goto :usage
        )
    )
)

set ETUICMD=%~dp0etui.cmd

reg.exe add "HKEY_LOCAL_MACHINE\SOFTWARE\Classes\*\shell\Open with Etui\command" /d """%ETUICMD%"" ""%%1"""

if not "%1" == "" (
    reg.exe add "HKEY_LOCAL_MACHINE\SOFTWARE\Classes\*\shell\Open with Etui" /v Position /d %1
)

goto end
:usage

    echo Adds an "Open with Etui" item to the File Explorer content menu.
    echo.
    echo registerExplorerMenu.cmd [Top^|Bottom]
    exit /B 1

:end
endlocal
