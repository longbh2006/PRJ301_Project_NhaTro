param(
    [ValidateRange(1024, 65535)][int]$Port = 8080,
    [string]$TomcatPath = 'C:/Program Files/Apache Software Foundation/Tomcat 10.1',
    [string]$AntPath = 'C:/Program Files/NetBeans-17/netbeans/extide/ant',
    [string]$JdkPath,
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$localPath = Join-Path $projectPath '.local'
$serverPath = Join-Path $localPath 'tomcat'
$pidPath = Join-Path $serverPath 'server.pid'

# Chỉ dừng tiến trình có đúng thư mục Tomcat riêng của project, tránh PID được tái sử dụng.
if (Test-Path -LiteralPath $pidPath) {
    $serverPid = [int](Get-Content -LiteralPath $pidPath -Raw).Trim()
    $runningServer = Get-CimInstance Win32_Process -Filter "ProcessId = $serverPid"
    if ($null -ne $runningServer) {
        if (!$runningServer.CommandLine.Contains("-Dcatalina.base=$serverPath")) {
            throw 'The saved PID belongs to another process; it was left running.'
        }
        Stop-Process -Id $serverPid
        Wait-Process -Id $serverPid -Timeout 15 -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $pidPath
}
if ($Stop) {
    Write-Output 'Project Tomcat stopped.'
    return
}

if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Port $Port is already in use. Choose another port with -Port."
}
if (!$JdkPath) {
    $JdkPath = Split-Path -Parent (Split-Path -Parent (Get-Command javac -ErrorAction Stop).Source)
}
$javaPath = Join-Path $JdkPath 'bin/java.exe'
$antLauncher = Join-Path $AntPath 'lib/ant-launcher.jar'
foreach ($requiredPath in @($javaPath, $antLauncher, (Join-Path $TomcatPath 'bin/bootstrap.jar'),
        (Join-Path $projectPath 'web/WEB-INF/ConnectDB.properties'),
        (Join-Path $projectPath 'web/WEB-INF/lib/mssql-jdbc-13.2.0.jre11.jar'))) {
    if (!(Test-Path -LiteralPath $requiredPath)) {
        throw "Required file is missing: $requiredPath"
    }
}

$buildPath = Join-Path $localPath 'build'
$distPath = Join-Path $localPath 'dist'
# Build sạch để file nguồn đã xóa không còn trong ứng dụng hoặc WAR.
$localRoot = [System.IO.Path]::GetFullPath($localPath).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
foreach ($generatedPath in @($buildPath, $distPath)) {
    $absolutePath = [System.IO.Path]::GetFullPath($generatedPath)
    if (!$absolutePath.StartsWith($localRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Generated output must stay inside this project .local directory.'
    }
    if (Test-Path -LiteralPath $absolutePath) {
        $resolvedPath = (Resolve-Path -LiteralPath $absolutePath).Path
        if (!$resolvedPath.StartsWith($localRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw 'Refusing to clean generated output outside this project .local directory.'
        }
        Remove-Item -LiteralPath $resolvedPath -Recurse -Force
    }
}
Push-Location $projectPath
try {
    & $javaPath -cp $antLauncher org.apache.tools.ant.launch.Launcher `
        "-Dbuild.dir=$buildPath" "-Ddist.dir=$distPath" "-Dj2ee.server.home=$TomcatPath" dist
    if ($LASTEXITCODE -ne 0) {
        throw 'Project build failed.'
    }
} finally {
    Pop-Location
}

foreach ($folder in @('conf', 'logs', 'temp', 'webapps', 'work')) {
    New-Item -ItemType Directory -Path (Join-Path $serverPath $folder) -Force | Out-Null
}
foreach ($configName in @('catalina.properties', 'logging.properties', 'web.xml', 'context.xml')) {
    Copy-Item -LiteralPath (Join-Path $TomcatPath "conf/$configName") `
        -Destination (Join-Path $serverPath "conf/$configName") -Force
}
$webPath = [System.Security.SecurityElement]::Escape((Join-Path $buildPath 'web').Replace('\', '/'))
$serverConfig = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" autoDeploy="false" deployOnStartup="true">
        <Context path="/Project_NhaTro" docBase="$webPath" reloadable="false" />
      </Host>
    </Engine>
  </Service>
</Server>
"@
[System.IO.File]::WriteAllText((Join-Path $serverPath 'conf/server.xml'), $serverConfig,
        (New-Object System.Text.UTF8Encoding($false)))

$serverArguments = @(
    "`"-Dcatalina.home=$TomcatPath`"",
    "`"-Dcatalina.base=$serverPath`"",
    "`"-Djava.io.tmpdir=$(Join-Path $serverPath 'temp')`"",
    "`"-Djava.util.logging.config.file=$(Join-Path $serverPath 'conf/logging.properties')`"",
    '-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager',
    '-cp', "`"$TomcatPath/bin/bootstrap.jar;$TomcatPath/bin/tomcat-juli.jar`"",
    'org.apache.catalina.startup.Bootstrap', 'start'
)
$serverProcess = Start-Process -FilePath $javaPath -ArgumentList $serverArguments `
    -WorkingDirectory $projectPath -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $serverPath 'logs/startup.out.log') `
    -RedirectStandardError (Join-Path $serverPath 'logs/startup.err.log')
$serverProcess.Id | Set-Content -LiteralPath $pidPath
$baseUrl = "http://127.0.0.1:$Port/Project_NhaTro"
$ready = $false
$deadline = (Get-Date).AddSeconds(40)
while ((Get-Date) -lt $deadline) {
    if ($serverProcess.HasExited) { break }
    try {
        $response = Invoke-WebRequest "$baseUrl/css/app.css" -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch {
        Start-Sleep -Milliseconds 250
    }
}
if (!$ready) {
    if (!$serverProcess.HasExited) { Stop-Process -Id $serverProcess.Id }
    Remove-Item -LiteralPath $pidPath -ErrorAction SilentlyContinue
    throw "Tomcat did not become ready. Check $serverPath/logs/startup.err.log"
}
Write-Output "Web ready: $baseUrl/rooms/detail?roomId=1"
Write-Output 'Run this script again to rebuild and restart; use -Stop to stop this project server.'
