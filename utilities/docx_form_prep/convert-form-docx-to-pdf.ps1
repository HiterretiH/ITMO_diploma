#Requires -Version 5.1
$ErrorActionPreference = 'Stop'

$d = (Resolve-Path (Join-Path $PSScriptRoot '..\..\backend\src\main\resources\templates\documents')).Path
$f = 'pdf:writer_pdf_Export:{"CreateForm":{"type":"boolean","value":"true"}}'
$tmp = Join-Path $env:TEMP ("docx-form-pdf-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmp -Force | Out-Null
try {
    foreach ($name in 'act_of_work', 'contract_application', 'waybill') {
        Copy-Item -LiteralPath (Join-Path $d "$name.form.docx") -Destination (Join-Path $tmp "$name.docx") -Force
        $in = Join-Path $tmp "$name.docx"
        Start-Process -FilePath 'soffice.exe' -ArgumentList '--headless', '--convert-to', $f, '--outdir', $d, $in -Wait -NoNewWindow
    }
}
finally {
    Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
