# create-debug-docs.ps1
# Collects all project source files into a flat folder + zip for Claude.ai upload
#
# Usage: .\create-debug-docs.ps1

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot

# ---- CONFIG ----

$IGNORE = @(
    'build', '.gradle', '.idea', '.git', 'debug_docs_',
    '.kotlin', 'intermediates', 'generated', 'node_modules'
)

$EXTENSIONS = @('.kt', '.kts', '.xml', '.toml', '.properties', '.md', '.json')

# ---- HELPERS ----

$script:usedNames = @{}

function Ignore($name) {
    if ($name.StartsWith('.')) { return $true }
    foreach ($p in $IGNORE) {
        if ($name -eq $p -or $name.StartsWith($p)) { return $true }
    }
    return $false
}

function IncludeFile($name) {
    $ext = [IO.Path]::GetExtension($name).ToLower()
    return $EXTENSIONS -contains $ext
}

function UniqueName($name) {
    if (-not $script:usedNames.ContainsKey($name)) {
        $script:usedNames[$name] = $true
        return $name
    }
    $ext = [IO.Path]::GetExtension($name)
    $stem = [IO.Path]::GetFileNameWithoutExtension($name)
    $i = 1
    while ($script:usedNames.ContainsKey("${stem}__$i$ext")) { $i++ }
    $script:usedNames["${stem}__$i$ext"] = $true
    return "${stem}__$i$ext"
}

function CopyFlat($src, $rel, $dest, $meta) {
    if (-not (Test-Path $src -PathType Container)) { return }
    
    Get-ChildItem $src -Force -ErrorAction SilentlyContinue | ForEach-Object {
        if (Ignore $_.Name) { return }
        $path = "$rel/$($_.Name)"
        
        if ($_.PSIsContainer) {
            CopyFlat $_.FullName $path $dest $meta
        }
        elseif (IncludeFile $_.Name) {
            $unique = UniqueName $_.Name
            Copy-Item $_.FullName "$dest\$unique" -Force
            [void]$meta.Add(@{ Original = $path; Debug = $unique })
        }
    }
}

function BuildTree($dir, $name) {
    $tree = @{ N = $name; T = 'd'; C = @() }
    if (-not (Test-Path $dir)) { return $tree }
    
    Get-ChildItem $dir -Force -ErrorAction SilentlyContinue | Where-Object { -not (Ignore $_.Name) } | 
    Sort-Object { -not $_.PSIsContainer }, Name | ForEach-Object {
        if ($_.PSIsContainer) {
            $tree.C += BuildTree $_.FullName $_.Name
        }
        elseif (IncludeFile $_.Name) {
            $tree.C += @{ N = $_.Name; T = 'f' }
        }
    }
    return $tree
}

function RenderTree($node, $pre, $last, $root) {
    $out = ""
    if ($root) {
        $out = $node.N + "/`n"
    } else {
        if ($last) { $conn = "+-- " } else { $conn = "|-- " }
        if ($node.T -eq 'd') { $suf = "/" } else { $suf = "" }
        $out = $pre + $conn + $node.N + $suf + "`n"
    }
    
    if ($node.C.Count -gt 0) {
        if ($root) { $cpre = "" }
        elseif ($last) { $cpre = $pre + "    " }
        else { $cpre = $pre + "|   " }
        
        for ($i = 0; $i -lt $node.C.Count; $i++) {
            $isLast = ($i -eq $node.C.Count - 1)
            $out += RenderTree $node.C[$i] $cpre $isLast $false
        }
    }
    return $out
}

# ---- MAIN ----

Write-Host ""
Write-Host "Book Reading Stats - Debug Docs" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

$ts = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$folder = "debug_docs_$ts"
$destPath = Join-Path $projectRoot $folder
New-Item -ItemType Directory -Path $destPath -Force | Out-Null

Write-Host ""
Write-Host "Collecting files..." -ForegroundColor Yellow

$meta = [System.Collections.ArrayList]::new()

# Collect from app/src/main
$srcMain = Join-Path $projectRoot "app\src\main"
CopyFlat $srcMain "app/src/main" $destPath $meta

# Collect root config files
$rootFiles = @(
    "app\build.gradle.kts",
    "build.gradle.kts", 
    "settings.gradle.kts",
    "gradle\libs.versions.toml",
    "gradle.properties",
    "README.md"
)

foreach ($file in $rootFiles) {
    $src = Join-Path $projectRoot $file
    if (Test-Path $src) {
        $name = [IO.Path]::GetFileName($file)
        $unique = UniqueName $name
        $destFile = Join-Path $destPath $unique
        Copy-Item $src $destFile -Force
        $relPath = $file -replace '\\', '/'
        [void]$meta.Add(@{ Original = $relPath; Debug = $unique })
    }
}

# Generate folder structure
Write-Host "Generating structure..." -ForegroundColor Yellow

$tree = BuildTree $projectRoot (Split-Path $projectRoot -Leaf)
$content = "Project: BookReadingStats`r`n"
$content += "Generated: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + "`r`n"
$content += "Files: " + $meta.Count + "`r`n"
$content += "========================================`r`n`r`n"
$content += RenderTree $tree "" $true $true
$content += "`r`n========================================`r`n"
$content += "FILE MAPPING (debug name -> original path)`r`n"
$content += "========================================`r`n`r`n"

$sorted = $meta | Sort-Object { $_.Original }
foreach ($item in $sorted) {
    $content += $item.Debug + "`r`n"
    $content += "  -> " + $item.Original + "`r`n`r`n"
}

$structFile = Join-Path $destPath "folder_structure.txt"
Set-Content $structFile $content -Encoding UTF8

# Create zip
Write-Host "Creating zip..." -ForegroundColor Yellow
$zipPath = Join-Path $destPath "$folder.zip"
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

# Get all files except the zip itself
$filesToZip = Get-ChildItem $destPath -File | Where-Object { $_.Extension -ne '.zip' }
Compress-Archive -Path $filesToZip.FullName -DestinationPath $zipPath -CompressionLevel Optimal

$size = [math]::Round((Get-Item $zipPath).Length / 1KB, 1)

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host "  Files: $($meta.Count)"
Write-Host "  Folder: $folder\"
Write-Host "  Zip:    $folder\$folder.zip ($size KB)"
Write-Host ""
Write-Host "Upload the .zip to Claude.ai for debugging!"
Write-Host ""
