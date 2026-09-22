# Stops the local portable PostgreSQL server
$pgsqlBin = Join-Path $PSScriptRoot "pgsql\bin\pg_ctl.exe"
$dataDir = Join-Path $PSScriptRoot "pgsql\data"

& $pgsqlBin -D $dataDir stop
