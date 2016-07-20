echo off
REM
REM  Read Device Address and display it in standard form.
REM
REM  Ex.  ./read_addr.cmd
REM
echo mem8 100000A4 6  > %TEMP%/script.jlink
echo r               >> %TEMP%/script.jlink
echo g               >> %TEMP%/script.jlink
echo exit            >> %TEMP%/script.jlink

JLink.Exe -if SWD -speed 1000 -device nRF51822_xxAC %TEMP%/script.jlink > %TEMP%/jlink_out.txt

REM
REM Get line of interest in jlink_out file
REM
findstr /C:"100000A4" %TEMP%\jlink_out.txt >> %TEMP%\grep_out.txt
set /p line=<%TEMP%\grep_out.txt
set line=%line:~-18%

del %TEMP%\script.jlink
del %TEMP%\jlink_out.txt
del %TEMP%\grep_out.txt

REM
REM reverse line string
REM
setlocal enabledelayedexpansion
set num=0
:LOOP
  call set tmp=%%line:~%num%,3%%%
  set /a num+=3
  if not "%tmp%" equ "" (
    set reverse=%tmp%%reverse%
    goto LOOP
  )

REM
REM trim white space from end of line
REM
set line=%reverse:~0,17%

REM
REM replace spaces with colons
REM
set line=!line: =:!

REM
REM  
REM
set nibble=%line:~0,1%
if %nibble%==0 set nibble=C
if %nibble%==1 set nibble=D
if %nibble%==2 set nibble=E
if %nibble%==3 set nibble=F
if %nibble%==4 set nibble=C
if %nibble%==5 set nibble=D
if %nibble%==6 set nibble=E
if %nibble%==7 set nibble=F
if %nibble%==8 set nibble=C
if %nibble%==9 set nibble=D
if %nibble%==A set nibble=E 
if %nibble%==B set nibble=F
if %nibble%==C set nibble=C
if %nibble%==D set nibble=D
if %nibble%==E set nibble=E
if %nibble%==F set nibble=F
set line=%nibble%%line:~1%

REM
REM Print line as label
REM
echo %line% >%TEMP%\label.txt
REM "notepad" /p %TEMP%\label.txt
"C:\ProgramData\Microsoft\Windows\Start Menu\Programs\Accessories\WordPad.lnk" /p %TEMP%\label.txt
del %TEMP%\label.txt
