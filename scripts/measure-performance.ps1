<#
.SYNOPSIS
    Measures how long the list endpoints take to answer (Windows PowerShell 5.1 or PowerShell 7).

.DESCRIPTION
    1. Logs in and gets a JWT.
    2. Reads how many records exist (so the result file can prove the "1,000 records" condition).
    3. For every endpoint: one warm-up call (not counted), then -Runs timed calls.
    4. Prints a Markdown table (min / average / p95 / max in milliseconds) that you can paste into docs/PERFORMANCE.md.

    The times include the network trip from YOUR computer to the server, which is what a real client sees.

.EXAMPLE
    # local application
    .\scripts\measure-performance.ps1 -BaseUrl http://localhost:8080 -Username admin1

.EXAMPLE
    # Railway application, 20 runs, save the table to a file
    .\scripts\measure-performance.ps1 -BaseUrl https://YOUR-APP.up.railway.app -Username admin1 -Runs 20 -OutFile docs\perf-railway.md
#>
param(
    [Parameter(Mandatory = $true)][string]$BaseUrl,
    [Parameter(Mandatory = $true)][string]$Username,
    [string]$Password,
    [int]$Runs = 10,
    [string]$OutFile = ""
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

# ---- password: ask for it (hidden) if it was not given ----
if (-not $Password) {
    $secure = Read-Host -Prompt "Password for $Username" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

# ---- 1) log in ----
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }
Write-Host "Logged in as $($login.username) ($($login.role))." -ForegroundColor Green

# ---- 2) how many records are there? ----
function Get-Total([string]$path) {
    (Invoke-RestMethod -Uri "$BaseUrl$path" -Headers $headers).totalElements
}
$drivers = Get-Total "/api/v1/drivers?size=1"
$rides = Get-Total "/api/v1/rides?size=1"
$types = Get-Total "/api/v1/vehicle-types?size=1"
Write-Host "Records in the database: drivers=$drivers, rides=$rides, vehicle types=$types" -ForegroundColor Cyan

# ---- 3) measure ----
function Measure-Endpoint([string]$name, [string]$path) {
    $url = "$BaseUrl$path"
    $null = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing   # warm-up, not counted
    $times = New-Object System.Collections.Generic.List[double]
    for ($i = 1; $i -le $Runs; $i++) {
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
        $watch.Stop()
        if ($response.StatusCode -ne 200) { throw "Unexpected status $($response.StatusCode) for $path" }
        $times.Add($watch.Elapsed.TotalMilliseconds)
    }
    $sorted = $times | Sort-Object
    $p95Index = [int][math]::Ceiling(0.95 * $sorted.Count) - 1
    $max = [math]::Round($sorted[$sorted.Count - 1], 1)
    [pscustomobject]@{
        Endpoint = $name
        Runs     = $Runs
        MinMs    = [math]::Round($sorted[0], 1)
        AvgMs    = [math]::Round(($times | Measure-Object -Average).Average, 1)
        P95Ms    = [math]::Round($sorted[$p95Index], 1)
        MaxMs    = $max
        Under500 = $(if ($max -lt 500) { "yes" } else { "NO" })
    }
}

$endpoints = @(
    @("GET /drivers  (page 0, size 20, sort id)",              "/api/v1/drivers?page=0&size=20&sortBy=id&direction=asc"),
    @("GET /drivers  (page 0, size 100, sort name)",           "/api/v1/drivers?page=0&size=100&sortBy=name&direction=asc"),
    @("GET /drivers  (deep page 40, size 20)",                 "/api/v1/drivers?page=40&size=20"),
    @("GET /drivers?available=true (size 20)",                 "/api/v1/drivers?available=true&page=0&size=20"),
    @("GET /rides  (size 20, newest first)",                   "/api/v1/rides?page=0&size=20&sortBy=bookingTime&direction=desc"),
    @("GET /rides  (size 100)",                                "/api/v1/rides?page=0&size=100"),
    @("GET /rides?status=COMPLETED (size 20)",                 "/api/v1/rides?status=COMPLETED&page=0&size=20&sortBy=bookingTime&direction=desc"),
    @("GET /vehicle-types  (size 20, sort name)",              "/api/v1/vehicle-types?page=0&size=20&sortBy=name"),
    @("GET /vehicle-types  (size 100)",                        "/api/v1/vehicle-types?page=0&size=100"),
    @("GET /drivers/with-rides  optimized (size 20)",          "/api/v1/drivers/with-rides?mode=optimized&page=0&size=20"),
    @("GET /drivers/with-rides  optimized (size 100)",         "/api/v1/drivers/with-rides?mode=optimized&page=0&size=100"),
    @("GET /drivers/with-rides  naive N+1 (size 20)",          "/api/v1/drivers/with-rides?mode=naive&page=0&size=20"),
    @("GET /drivers/with-rides  naive N+1 (size 100)",         "/api/v1/drivers/with-rides?mode=naive&page=0&size=100")
)

$results = foreach ($e in $endpoints) {
    Write-Host "Measuring $($e[0]) ..."
    Measure-Endpoint $e[0] $e[1]
}

# ---- 4) output ----
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("Measured on $(Get-Date -Format 'yyyy-MM-dd HH:mm') against $BaseUrl")
$lines.Add("Records: drivers=$drivers, rides=$rides, vehicle types=$types. Runs per endpoint: $Runs (after 1 warm-up call).")
$lines.Add("")
$lines.Add("| Endpoint | Runs | Min ms | Avg ms | p95 ms | Max ms | All under 500 ms? |")
$lines.Add("|---|---|---|---|---|---|---|")
foreach ($r in $results) {
    $lines.Add("| $($r.Endpoint) | $($r.Runs) | $($r.MinMs) | $($r.AvgMs) | $($r.P95Ms) | $($r.MaxMs) | $($r.Under500) |")
}
$lines | ForEach-Object { Write-Host $_ }

if ($OutFile) {
    $lines | Set-Content -Path $OutFile -Encoding UTF8
    Write-Host "Saved to $OutFile" -ForegroundColor Green
}
