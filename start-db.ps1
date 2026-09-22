# Starts the local portable PostgreSQL server
$pgsqlBin = Join-Path $PSScriptRoot "pgsql\bin\pg_ctl.exe"
$dataDir = Join-Path $PSScriptRoot "pgsql\data"
$logFile = Join-Path $PSScriptRoot "pgsql\logfile.log"

& $pgsqlBin -D $dataDir -l $logFile start
