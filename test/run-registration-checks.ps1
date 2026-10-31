param(
    [Parameter(Mandatory = $true)][string]$TomcatHome,
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [string]$DatabaseConfig,
    [int]$Port = 18030,
    [switch]$KeepServer
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
if (!$DatabaseConfig) { $DatabaseConfig = Join-Path $projectRoot 'web/WEB-INF/ConnectDB.properties' }
if (!(Test-Path -LiteralPath $DatabaseConfig)) { throw 'Provide a local ConnectDB.properties file.' }
$checkRoot = Join-Path $env:TEMP ('nhatro-nt30-' + [guid]::NewGuid().ToString('N'))
$serverRoot = Join-Path $checkRoot 'tomcat'
$appRoot = Join-Path $serverRoot 'webapps/nt30'
$classes = Join-Path $appRoot 'WEB-INF/classes'
New-Item -ItemType Directory -Force -Path $classes,(Join-Path $serverRoot 'conf'),(Join-Path $serverRoot 'logs'),(Join-Path $serverRoot 'temp'),(Join-Path $serverRoot 'work') | Out-Null
Copy-Item (Join-Path $projectRoot 'web/*') $appRoot -Recurse -Force
Copy-Item -LiteralPath $DatabaseConfig -Destination (Join-Path $appRoot 'WEB-INF/ConnectDB.properties') -Force
foreach ($configName in @('catalina.properties','logging.properties','web.xml')) {
    Copy-Item -LiteralPath (Join-Path $TomcatHome "conf/$configName") -Destination (Join-Path $serverRoot 'conf')
}
$serverXml = '<Server port="-1"><Service name="Catalina"><Connector address="127.0.0.1" port="{0}" protocol="HTTP/1.1"/><Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false"/></Engine></Service></Server>' -f $Port
[IO.File]::WriteAllText((Join-Path $serverRoot 'conf/server.xml'), $serverXml)
$sources = @(Get-ChildItem (Join-Path $projectRoot 'src/java'),(Join-Path $projectRoot 'test') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JavaHome 'bin/javac.exe') --release 17 -encoding UTF-8 -Xlint:all -cp (Join-Path $TomcatHome 'lib/servlet-api.jar') -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
& (Join-Path $JavaHome 'bin/java.exe') -cp $classes util.PasswordHasherTest
if ($LASTEXITCODE -ne 0) { throw 'PasswordHasher tests failed.' }
& (Join-Path $JavaHome 'bin/java.exe') -cp $classes service.AuthServiceValidationTest
if ($LASTEXITCODE -ne 0) { throw 'Validation tests failed.' }
$javaArguments = @('-Dcatalina.home="' + $TomcatHome + '"', '-Dcatalina.base="' + $serverRoot + '"', '-Djava.io.tmpdir="' + (Join-Path $serverRoot 'temp') + '"', '-cp', '"' + (Join-Path $TomcatHome 'bin/bootstrap.jar') + ';' + (Join-Path $TomcatHome 'bin/tomcat-juli.jar') + '"', 'org.apache.catalina.startup.Bootstrap', 'start')
$serverProcess = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $javaArguments -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $checkRoot 'stdout.log') -RedirectStandardError (Join-Path $checkRoot 'stderr.log')
Add-Type -AssemblyName System.Net.Http
$handler = New-Object Net.Http.HttpClientHandler
$handler.AllowAutoRedirect = $false
$client = New-Object Net.Http.HttpClient($handler)
$baseUrl = "http://127.0.0.1:$Port/nt30"
$httpUsername = 'nt30_http_' + [guid]::NewGuid().ToString('N').Substring(0,12)
$completed = $false
function Assert-Check([bool]$Passed, [string]$Message) { if (!$Passed) { throw $Message } }
function Send-Form([string]$Url, [hashtable]$Values) {
    $dictionary = New-Object 'Collections.Generic.Dictionary[string,string]'
    foreach ($entry in $Values.GetEnumerator()) { $dictionary.Add([string]$entry.Key, [string]$entry.Value) }
    $content = New-Object Net.Http.FormUrlEncodedContent -ArgumentList (, $dictionary)
    try { return $client.PostAsync($Url, $content).GetAwaiter().GetResult() } finally { $content.Dispose() }
}
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        if ($serverProcess.HasExited) { throw "Tomcat stopped. See $checkRoot/stderr.log" }
        try {
            $response = $client.GetAsync("$baseUrl/register").GetAwaiter().GetResult()
            $ready = [int]$response.StatusCode -eq 200
        } catch { $ready = $false }
        if ($ready) { break }
        Start-Sleep -Milliseconds 500
    }
    Assert-Check $ready "Register page not ready. See $checkRoot/stderr.log"
    $page = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    Assert-Check ($page.Contains('Tạo tài khoản người thuê')) 'GET form failed.'
    $checks = $client.GetAsync("$baseUrl/checks").GetAwaiter().GetResult()
    $checkResult = $checks.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    Assert-Check ([int]$checks.StatusCode -eq 200) "Integration checks failed: $checkResult See $checkRoot/stdout.log"
    Write-Output $checkResult.Trim()
    $invalid = Send-Form "$baseUrl/register" @{username='ab';password='short';fullName='<img src=x onerror=alert(1)>';idCardNo='123'}
    $invalidPage = $invalid.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    Assert-Check ([int]$invalid.StatusCode -eq 200 -and $invalidPage.Contains('CCCD phải gồm đúng 12 chữ số.')) 'Server validation failed.'
    Assert-Check ($invalidPage.Contains('&lt;img src=x onerror=alert(1)&gt;') -and !$invalidPage.Contains('<img src=x onerror=alert(1)>')) 'XSS escaping failed.'
    Assert-Check (!$invalidPage.Contains('value="short"')) 'Password was reflected.'
    Assert-Check ($invalidPage.Contains('value="ab"')) 'Form data was not retained.'
    $values = @{username=$httpUsername;password='Test password 123';fullName='Nguyễn Thị Ánh';idCardNo=('7' + (Get-Random -Minimum 100000 -Maximum 999999).ToString() + (Get-Random -Minimum 10000 -Maximum 99999).ToString());role='ADMIN';accountId='1'}
    $success = Send-Form "$baseUrl/register" $values
    Assert-Check ([int]$success.StatusCode -eq 302 -and $success.Headers.Location.OriginalString.EndsWith('/nt30/login?registered=1')) 'PRG redirect failed.'
    $persisted = $client.GetAsync("$baseUrl/checks?username=$httpUsername").GetAwaiter().GetResult()
    Assert-Check ([int]$persisted.StatusCode -eq 200) 'HTTP persistence/role tampering check failed.'
    $duplicate = Send-Form "$baseUrl/register" $values
    $duplicatePage = $duplicate.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    Assert-Check ($duplicatePage.Contains('Tên đăng nhập đã được sử dụng.')) 'Duplicate username error missing.'
    Assert-Check ($duplicatePage.Contains('Nguyễn Thị Ánh') -and !$duplicatePage.Contains('value="Test password 123"')) 'Duplicate form data/password handling failed.'
    $privateView = $client.GetAsync("$baseUrl/WEB-INF/views/register.jsp").GetAwaiter().GetResult()
    Assert-Check ([int]$privateView.StatusCode -eq 404) 'WEB-INF view must be private.'
    Write-Output 'PASS HTTP: GET/JSP, validation, XSS, retained fields, blank password, PRG, duplicate username, role tampering and WEB-INF access'
    $completed = $true
} finally {
    try {
        $cleanup = Send-Form "$baseUrl/checks?username=$httpUsername" @{}
        Assert-Check ([int]$cleanup.StatusCode -eq 302) 'HTTP fixture cleanup failed.'
    } finally {
        $client.Dispose()
        $handler.Dispose()
        if (!$KeepServer -or !$completed) { Stop-Process -Id $serverProcess.Id -ErrorAction SilentlyContinue }
        Write-Output "Evidence: $checkRoot"
        if ($KeepServer -and $completed) { Write-Output "Test server PID: $($serverProcess.Id); URL: $baseUrl/register" }
    }
}
