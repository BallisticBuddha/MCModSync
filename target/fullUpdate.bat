@echo off
setlocal

IF EXIST "C:\Program Files\Java\jre7\bin\java.exe" (
	SET JAVA_LOCATION="C:\Program Files\Java\jre7\bin\java"
) ELSE (
	IF EXIST "C:\Program Files (x86)\Java\jre7\bin\java.exe" (
		SET JAVA_LOCATION="C:\Program Files (x86)\Java\jre7\bin\java"
	) ELSE (
		SET JAVA_LOCATION="java"
	)
)
%JAVA_LOCATION% -jar MCModSync.jar -d -f
echo "Running Forge Installer, follow the instuctions to install the forge client there..."
%JAVA_LOCATION% -jar ForgeInstaller.jar
del ForgeInstaller.jar
pause
exit /b
