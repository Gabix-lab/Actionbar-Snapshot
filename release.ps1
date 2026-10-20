param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

$ErrorActionPreference = "Stop"

$Repo = "Gabix-lab/Actionbar-Snapshot"
$GradleProps = "gradle.properties"

if ($Version -notmatch '^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$') {
    Write-Host "HIBA: Rossz verzio formatum!" -ForegroundColor Red
    Write-Host "Pelda: .\release.ps1 -Version 1.6.4" -ForegroundColor Yellow
    Write-Host "       .\release.ps1 -Version 1.6.4-beta.1" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $GradleProps)) {
    Write-Host "HIBA: $GradleProps nem talalhato a jelenlegi mappaban!" -ForegroundColor Red
    Write-Host "A projekt gyokerebol futtasd:" -ForegroundColor Yellow
    Write-Host "  cd 'C:\Users\User\IdeaProjects\Actionbar Snapshot'" -ForegroundColor Yellow
    exit 1
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "HIBA: gh CLI nincs telepitve! winget install --id GitHub.cli" -ForegroundColor Red
    exit 1
}

if (-not $env:JAVA_HOME) {
    Write-Host "HIBA: JAVA_HOME nincs beallitva!" -ForegroundColor Red
    Write-Host "Allitsd be a JDK 21 mappara (zarojel nelkul), pl:" -ForegroundColor Yellow
    Write-Host '  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"' -ForegroundColor Yellow
    exit 1
}

$env:JAVA_HOME = $env:JAVA_HOME.TrimEnd('\')
if (-not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    Write-Host "HIBA: JAVA_HOME ervenytelen: $env:JAVA_HOME" -ForegroundColor Red
    Write-Host "Nem talalhato benne: bin\java.exe" -ForegroundColor Yellow
    exit 1
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green

$repoOwner = ($Repo -split '/')[0]
$ghUser = (gh api user --jq .login 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $ghUser) {
    Write-Host "HIBA: gh nincs bejelentkezve! Futtasd: gh auth login" -ForegroundColor Red
    exit 1
}

if ($ghUser -ne $repoOwner) {
    Write-Host "FIGYELEM: gh fiok ($ghUser) != repo tulajdonos ($repoOwner)" -ForegroundColor Yellow
    $hasPush = gh api "repos/$Repo" --jq .permissions.push 2>$null
    if ($LASTEXITCODE -ne 0 -or $hasPush -ne "true") {
        Write-Host "HIBA: A(z) $ghUser fioknak nincs irasi joga a $Repo repohoz!" -ForegroundColor Red
        Write-Host "Megoldas:" -ForegroundColor Yellow
        Write-Host "  gh auth logout ; gh auth login   (mint $repoOwner)" -ForegroundColor Yellow
        Write-Host "  vagy add hozza $ghUser-t collaboratorkent a GitHubon" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "Van irasi jog, folytatas..." -ForegroundColor Green
}

Write-Host "Release v$Version..." -ForegroundColor Cyan
Write-Host ""

Write-Host "Verzio frissitese: $GradleProps..." -ForegroundColor Yellow
$content = Get-Content $GradleProps -Raw
$content = $content -replace '(?m)^(\s*mod_version\s*=\s*).*$', "`${1}$Version"
Set-Content $GradleProps -Value $content -NoNewline

Write-Host "Build..." -ForegroundColor Yellow
.\gradlew clean build --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "HIBA: Build sikertelen!" -ForegroundColor Red
    exit 1
}

$jar = Get-ChildItem "build/libs" -Filter "*.jar" |
        Where-Object { $_.Name -notmatch '-(sources|dev|raw|javadoc)\.jar$' } |
        Sort-Object Length -Descending |
        Select-Object -First 1

if (-not $jar) {
    Write-Host "HIBA: Nem talalhato jar a build/libs mappaban!" -ForegroundColor Red
    Get-ChildItem "build/libs" -Filter "*.jar" -ErrorAction SilentlyContinue |
        ForEach-Object { Write-Host "  $($_.Name)" }
    exit 1
}

Write-Host "Jar: $($jar.Name) ($([math]::Round($jar.Length / 1KB)) KB)" -ForegroundColor Green

Write-Host "Git commit + tag + push..." -ForegroundColor Yellow

$existingTag = git tag -l "v$Version"
if ($existingTag) {
    Write-Host "HIBA: A v$Version tag mar letezik!" -ForegroundColor Red
    Write-Host "Torles: git tag -d v$Version ; git push origin :refs/tags/v$Version" -ForegroundColor Yellow
    Write-Host "  vagy hasznalj uj verziot." -ForegroundColor Yellow
    exit 1
}

git add $GradleProps
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m "Release v$Version"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "HIBA: git commit sikertelen!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Nincs valtozas, commit kihagyva." -ForegroundColor Yellow
}

git tag "v$Version"
git push
if ($LASTEXITCODE -ne 0) {
    Write-Host "HIBA: git push sikertelen!" -ForegroundColor Red
    exit 1
}
git push origin "v$Version"
if ($LASTEXITCODE -ne 0) {
    Write-Host "HIBA: tag push sikertelen!" -ForegroundColor Red
    exit 1
}

Write-Host "GitHub Release..." -ForegroundColor Yellow
gh release create "v$Version" "$($jar.FullName)" `
    --repo $Repo `
    --title "v$Version" `
    --notes "Release v$Version"

if ($LASTEXITCODE -ne 0) {
    Write-Host "HIBA: Release sikertelen!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "KESZ! v$Version publikalva!" -ForegroundColor Green
Write-Host "Letoltes: https://github.com/$Repo/releases/latest" -ForegroundColor Cyan