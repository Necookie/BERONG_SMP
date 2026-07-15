param(
    [string]$MinecraftDir = "$env:APPDATA\.minecraft"
)

$ErrorActionPreference = "Stop"

# $MyInvocation.MyCommand.Path / $PSScriptRoot do not reliably resolve inside a
# ps2exe-compiled binary (they can come back empty), which crashes the very
# first Split-Path call before any output has a chance to stay on screen -
# looks like the window "flashing and closing instantly". Fall back to the
# running process's own exe path, which is always correct for a compiled exe.
if ($PSScriptRoot -and (Test-Path $PSScriptRoot)) {
    $ScriptDir = $PSScriptRoot
} else {
    $ScriptDir = Split-Path -Parent ([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName)
}
$PayloadDir = Join-Path $ScriptDir "payload"
$NeoForgeInstaller = Join-Path $PayloadDir "neoforge-26.1.2.80-installer.jar"
$ModJar = Join-Path $PayloadDir "berongsmp-1.0.0.jar"

function Write-Step($msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Fail($msg) {
    Write-Host ""
    Write-Host "ERROR: $msg" -ForegroundColor Red
    Write-Host ""
    throw $msg
}

function Find-Java($MinecraftDir) {
    $candidates = @()

    # Official launcher (minecraft.net installer), classic install location
    $candidates += Get-ChildItem "$MinecraftDir\runtime\*\windows-x64\*\bin\javaw.exe" -ErrorAction SilentlyContinue
    # Official launcher via Program Files
    $candidates += Get-ChildItem "C:\Program Files (x86)\Minecraft Launcher\runtime\*\windows-x64\*\bin\javaw.exe" -ErrorAction SilentlyContinue
    # Microsoft Store version of the launcher
    $candidates += Get-ChildItem "$env:LOCALAPPDATA\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\*\windows-x64\*\bin\javaw.exe" -ErrorAction SilentlyContinue

    foreach ($c in $candidates) {
        if ($c) { return $c.FullName }
    }

    # Fall back to whatever's on PATH
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    return $null
}

# Everything is wrapped so ANY unexpected error still prints and pauses
# instead of the window flashing shut before it can be read.
try {
    Write-Host "======================================" -ForegroundColor Green
    Write-Host "  BerongSMP Client Installer" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Green

    # -----------------------------------------------------------------------
    # 1. Confirm Minecraft has actually been run before (this is the one
    #    thing that genuinely cannot be automated - a real Minecraft Java
    #    Edition account + the official Launcher having been opened at least
    #    once, which is also what provisions the bundled Java runtime this
    #    script relies on).
    # -----------------------------------------------------------------------
    Write-Step "Checking for an existing Minecraft installation..."
    if (-not (Test-Path $MinecraftDir)) {
        Write-Fail "Could not find a Minecraft installation at `"$MinecraftDir`".`n`nOpen the official Minecraft Launcher and press Play at least once (any version), then run this installer again."
    }
    Write-Host "Found: $MinecraftDir"

    # -----------------------------------------------------------------------
    # 2. Locate a Java runtime. Prefer the one bundled with the official
    #    Minecraft Launcher (present once Minecraft has been launched at
    #    least once) over a system PATH java, since it's guaranteed to be
    #    present and a known-good version.
    # -----------------------------------------------------------------------
    Write-Step "Looking for a Java runtime..."

    $JavaExe = Find-Java $MinecraftDir
    if (-not $JavaExe) {
        Write-Fail "Could not find a Java runtime anywhere (checked the Minecraft Launcher's bundled runtime and your system PATH).`n`nOpen the official Minecraft Launcher and press Play at least once, then run this installer again."
    }
    Write-Host "Using Java: $JavaExe"

    # -----------------------------------------------------------------------
    # 3. Silently install the NeoForge client loader.
    # -----------------------------------------------------------------------
    Write-Step "Installing NeoForge 26.1.2.80 (this can take a minute)..."
    if (-not (Test-Path $NeoForgeInstaller)) {
        Write-Fail "Missing bundled file: payload\neoforge-26.1.2.80-installer.jar`n`nThis installer package is incomplete - re-download it."
    }

    $proc = Start-Process -FilePath $JavaExe `
        -ArgumentList @("-jar", "`"$NeoForgeInstaller`"", "--install-client", "`"$MinecraftDir`"") `
        -NoNewWindow -PassThru -Wait

    if ($proc.ExitCode -ne 0) {
        Write-Fail "NeoForge installer exited with code $($proc.ExitCode). Check the output above for details."
    }
    Write-Host "NeoForge client installed."

    # -----------------------------------------------------------------------
    # 4. Drop the mod jar into the mods folder (creating it if needed - this
    #    is the exact step that fails when done by hand via the Run dialog,
    #    since Win+R can only open a folder that already exists).
    # -----------------------------------------------------------------------
    Write-Step "Installing the BerongSMP mod..."
    if (-not (Test-Path $ModJar)) {
        Write-Fail "Missing bundled file: payload\berongsmp-1.0.0.jar`n`nThis installer package is incomplete - re-download it."
    }

    $ModsDir = Join-Path $MinecraftDir "mods"
    if (-not (Test-Path $ModsDir)) {
        New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
    }
    Copy-Item -Path $ModJar -Destination $ModsDir -Force
    Write-Host "Copied berongsmp-1.0.0.jar to $ModsDir"

    # -----------------------------------------------------------------------
    # Done.
    # -----------------------------------------------------------------------
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Green
    Write-Host "  Install complete!" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:"
    Write-Host "  1. Open the Minecraft Launcher"
    Write-Host "  2. In the profile dropdown, select 'NeoForge 26.1.2.80' and click Play"
    Write-Host "  3. Multiplayer -> Add Server -> paste the server address you were given -> Join"
}
catch {
    Write-Host ""
    Write-Host "Something went wrong:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
finally {
    Write-Host ""
    Read-Host "Press Enter to close"
}
